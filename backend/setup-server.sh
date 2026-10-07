#!/usr/bin/env bash
# ==============================================================================
# Mestxa Bare-Metal Relay Server Setup Script
# Target: Ubuntu 22.04 / 24.04 LTS or Debian 12 (Bare Metal / Dedicated / VPS)
# Architecture: NO DOCKER. Direct Native Binaries + Systemd + Caddy + Redis Socket + Coturn
# Domain: api.mestxa.com
# ==============================================================================

set -euo pipefail

echo "=========================================================="
echo " Starting Mestxa Production Bare-Metal Server Provisioning "
echo "=========================================================="

# 1. Update OS and Install Core Dependencies
echo "[1/7] Updating system and installing base tooling..."
export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get upgrade -y
apt-get install -y curl ufw redis-server coturn libcap2-bin build-essential pkg-config libssl-dev

# 2. Kernel & Network Tuning for High-Concurrency WebSockets (1M+ Sockets)
echo "[2/7] Applying high-throughput sysctl kernel optimizations..."
cat << 'EOF' > /etc/sysctl.d/99-mestxa.conf
# File descriptors and socket limits
fs.file-max = 2097152
fs.nr_open = 2097152

# Socket backlog and connection queues
net.core.somaxconn = 65535
net.ipv4.tcp_max_syn_backlog = 65535
net.core.netdev_max_backlog = 65536

# Port range expansion
net.ipv4.ip_local_port_range = 1024 65535

# TCP memory and buffer tuning
net.ipv4.tcp_rmem = 4096 87380 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216

# Fast recycling and TIME_WAIT handling
net.ipv4.tcp_tw_reuse = 1
net.ipv4.tcp_fin_timeout = 15
EOF
sysctl --system

# 3. Configure Redis for Ultra-Low Latency Unix Domain Sockets
echo "[3/7] Configuring Redis on Unix Domain Socket (/var/run/redis/redis.sock)..."
mkdir -p /var/run/redis
chown -R redis:redis /var/run/redis

cat << 'EOF' >> /etc/redis/redis.conf
# Mestxa Unix Domain Socket Configuration
unixsocket /var/run/redis/redis.sock
unixsocketperm 770
# Disable TCP binding for zero network overhead
port 0
# Ephemeral memory management
maxmemory 2gb
maxmemory-policy allkeys-lru
save ""
appendonly no
EOF

systemctl restart redis-server
systemctl enable redis-server

# 4. Install & Configure Caddy Server (Automated Zero-Config TLS)
echo "[4/7] Installing Caddy Web Server..."
apt-get install -y debian-keyring debian-archive-keyring apt-transport-https
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg --yes
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | tee /etc/apt/sources.list.d/caddy-stable.list
apt-get update -y
apt-get install -y caddy

mkdir -p /etc/caddy
cat << 'EOF' > /etc/caddy/Caddyfile
api.mestxa.com {
    encode zstd gzip

    header {
        Strict-Transport-Security "max-age=63072000; includeSubDomains; preload"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Referrer-Policy "no-referrer"
    }

    # Reverse proxy WebSockets and REST directly to the native Axum service
    reverse_proxy 127.0.0.1:8080 {
        header_up Host {host}
        header_up X-Real-IP {remote}
        header_up X-Forwarded-For {remote}
        header_up X-Forwarded-Proto {scheme}
    }
}
EOF
systemctl restart caddy
systemctl enable caddy

# 5. Configure Coturn STUN/TURN for WebRTC Studio-Quality Calling
echo "[5/7] Configuring Coturn STUN/TURN server..."
TURN_SECRET=$(head -c 32 /dev/urandom | base64)

cat << EOF > /etc/turnserver.conf
listening-port=3478
tls-listening-port=5349
fingerprint
lt-cred-mech
use-auth-secret
static-auth-secret=${TURN_SECRET}
realm=api.mestxa.com
min-port=49152
max-port=65535
log-file=/var/log/turnserver.log
no-cli
no-tcp
stale-nonce
EOF

# Enable Coturn in /etc/default/coturn
sed -i 's/#TURNSERVER_ENABLED=1/TURNSERVER_ENABLED=1/' /etc/default/coturn || true
systemctl restart coturn
systemctl enable coturn

# 6. Setup Mestxa System User, Directory, and Systemd Service
echo "[6/7] Setting up mestxa system user and deployment directories..."
useradd -r -s /usr/sbin/nologin -d /var/lib/mestxa mestxa || true
usermod -a -G redis mestxa
mkdir -p /opt/mestxa /var/lib/mestxa
chown -R mestxa:mestxa /opt/mestxa /var/lib/mestxa

# Create Environment File
cat << 'EOF' > /etc/default/mestxa-relay
RUST_LOG=info
MESTXA_HOST=127.0.0.1
MESTXA_PORT=8080
REDIS_SOCKET=/var/run/redis/redis.sock
EOF

cat << 'EOF' > /etc/systemd/system/mestxa-relay.service
[Unit]
Description=Mestxa Zero-Knowledge Bare-Metal Message Relay
After=network.target redis-server.service
Wants=redis-server.service

[Service]
Type=simple
User=mestxa
Group=mestxa
WorkingDirectory=/opt/mestxa
EnvironmentFile=/etc/default/mestxa-relay
ExecStart=/opt/mestxa/mestxa-relay
Restart=always
RestartSec=2s

# Hardened Security Sandboxing
LimitNOFILE=1048576
ProtectSystem=full
ProtectHome=true
NoNewPrivileges=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload

# 7. Configure Hardware Firewall (UFW)
echo "[7/7] Hardening network firewall with UFW..."
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp comment 'SSH Management'
ufw allow 80/tcp comment 'HTTP ACME Challenge'
ufw allow 443/tcp comment 'HTTPS API & WebSockets'
ufw allow 3478/udp comment 'STUN/TURN WebRTC'
ufw allow 5349/udp comment 'TURNS WebRTC Secure'
ufw allow 49152:65535/udp comment 'WebRTC Peer Media Ports'
ufw --force enable

echo "=========================================================="
echo " Mestxa Bare-Metal Server Provisioning Completed!         "
echo " WebRTC STUN/TURN Auth Secret: ${TURN_SECRET}             "
echo " Binary Target: /opt/mestxa/mestxa-relay                  "
echo " Start Service: systemctl start mestxa-relay              "
echo " Status Check:  systemctl status mestxa-relay             "
echo "=========================================================="
