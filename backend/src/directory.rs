//! Mestxa account directory: country-aware MX number allocation, username
//! reservation, and handle lookup (@username or MX number -> identity key).
//!
//! MX numbers look like `MX-<calling code>-<subscriber digits>`, where the
//! subscriber length mirrors real local numbers in that country
//! (e.g. Nigeria `MX-234-8123456789`, UK `MX-44-7123456789`).
//!
//! Numbers are issued by the relay (never the client) so they are globally
//! unique, and are never recycled once registered.

use rand::Rng;
use redis::aio::ConnectionManager;
use redis::AsyncCommands;
use serde::Serialize;
use std::sync::Arc;

/// How long an offered number is held for the user before it is released.
pub const OFFER_TTL_SECS: u64 = 300;

/// ISO-3166 alpha-2 -> (country name, calling code, national subscriber length)
pub const COUNTRIES: &[(&str, &str, &str, usize)] = &[
    // Africa
    ("NG", "Nigeria", "234", 10),
    ("GH", "Ghana", "233", 9),
    ("KE", "Kenya", "254", 9),
    ("ZA", "South Africa", "27", 9),
    ("EG", "Egypt", "20", 10),
    ("ET", "Ethiopia", "251", 9),
    ("TZ", "Tanzania", "255", 9),
    ("UG", "Uganda", "256", 9),
    ("RW", "Rwanda", "250", 9),
    ("CM", "Cameroon", "237", 9),
    ("CI", "Côte d'Ivoire", "225", 10),
    ("SN", "Senegal", "221", 9),
    ("ZM", "Zambia", "260", 9),
    ("ZW", "Zimbabwe", "263", 9),
    ("MA", "Morocco", "212", 9),
    ("DZ", "Algeria", "213", 9),
    ("TN", "Tunisia", "216", 8),
    ("BJ", "Benin", "229", 10),
    ("TG", "Togo", "228", 8),
    ("ML", "Mali", "223", 8),
    ("BF", "Burkina Faso", "226", 8),
    ("NE", "Niger", "227", 8),
    ("LR", "Liberia", "231", 9),
    ("SL", "Sierra Leone", "232", 8),
    ("GM", "Gambia", "220", 7),
    ("AO", "Angola", "244", 9),
    ("MZ", "Mozambique", "258", 9),
    ("MW", "Malawi", "265", 9),
    ("BW", "Botswana", "267", 8),
    ("NA", "Namibia", "264", 9),
    ("CD", "DR Congo", "243", 9),
    ("SD", "Sudan", "249", 9),
    ("LY", "Libya", "218", 9),
    ("SO", "Somalia", "252", 9),
    // Americas
    ("US", "United States", "1", 10),
    ("CA", "Canada", "1", 10),
    ("JM", "Jamaica", "1", 10),
    ("TT", "Trinidad and Tobago", "1", 10),
    ("MX", "Mexico", "52", 10),
    ("BR", "Brazil", "55", 11),
    ("AR", "Argentina", "54", 10),
    ("CO", "Colombia", "57", 10),
    ("CL", "Chile", "56", 9),
    ("PE", "Peru", "51", 9),
    ("VE", "Venezuela", "58", 10),
    // Europe
    ("GB", "United Kingdom", "44", 10),
    ("IE", "Ireland", "353", 9),
    ("FR", "France", "33", 9),
    ("DE", "Germany", "49", 11),
    ("IT", "Italy", "39", 10),
    ("ES", "Spain", "34", 9),
    ("PT", "Portugal", "351", 9),
    ("NL", "Netherlands", "31", 9),
    ("BE", "Belgium", "32", 9),
    ("CH", "Switzerland", "41", 9),
    ("AT", "Austria", "43", 10),
    ("SE", "Sweden", "46", 9),
    ("NO", "Norway", "47", 8),
    ("DK", "Denmark", "45", 8),
    ("FI", "Finland", "358", 9),
    ("PL", "Poland", "48", 9),
    ("CZ", "Czechia", "420", 9),
    ("RO", "Romania", "40", 9),
    ("GR", "Greece", "30", 10),
    ("HU", "Hungary", "36", 9),
    ("UA", "Ukraine", "380", 9),
    ("RU", "Russia", "7", 10),
    ("TR", "Türkiye", "90", 10),
    // Middle East
    ("AE", "United Arab Emirates", "971", 9),
    ("SA", "Saudi Arabia", "966", 9),
    ("QA", "Qatar", "974", 8),
    ("KW", "Kuwait", "965", 8),
    ("BH", "Bahrain", "973", 8),
    ("OM", "Oman", "968", 8),
    ("IL", "Israel", "972", 9),
    ("JO", "Jordan", "962", 9),
    ("LB", "Lebanon", "961", 8),
    ("IQ", "Iraq", "964", 10),
    ("IR", "Iran", "98", 10),
    // Asia-Pacific
    ("IN", "India", "91", 10),
    ("PK", "Pakistan", "92", 10),
    ("BD", "Bangladesh", "880", 10),
    ("LK", "Sri Lanka", "94", 9),
    ("NP", "Nepal", "977", 10),
    ("CN", "China", "86", 11),
    ("JP", "Japan", "81", 10),
    ("KR", "South Korea", "82", 10),
    ("ID", "Indonesia", "62", 10),
    ("MY", "Malaysia", "60", 9),
    ("SG", "Singapore", "65", 8),
    ("PH", "Philippines", "63", 10),
    ("TH", "Thailand", "66", 9),
    ("VN", "Vietnam", "84", 9),
    ("HK", "Hong Kong", "852", 8),
    ("AU", "Australia", "61", 9),
    ("NZ", "New Zealand", "64", 9),
];

const RESERVED_USERNAMES: &[&str] = &[
    "mestxa", "admin", "administrator", "support", "help", "official", "root",
    "system", "security", "team", "staff", "moderator", "api", "null", "undefined",
];

#[derive(Serialize, Clone)]
pub struct CountryInfo {
    pub iso: &'static str,
    pub name: &'static str,
    pub calling_code: &'static str,
    pub digits: usize,
}

pub fn country(iso: &str) -> Option<CountryInfo> {
    let iso = iso.trim().to_uppercase();
    COUNTRIES
        .iter()
        .find(|c| c.0 == iso)
        .map(|c| CountryInfo { iso: c.0, name: c.1, calling_code: c.2, digits: c.3 })
}

pub fn all_countries() -> Vec<CountryInfo> {
    COUNTRIES
        .iter()
        .map(|c| CountryInfo { iso: c.0, name: c.1, calling_code: c.2, digits: c.3 })
        .collect()
}

/// Canonical form: `MX-<code>-<digits>`. Accepts `mx 234 812...`, `+234812...` is NOT
/// accepted because the calling code boundary would be ambiguous.
pub fn normalize_number(raw: &str) -> Option<String> {
    let up = raw.trim().to_uppercase().replace(' ', "-");
    let rest = up.strip_prefix("MX-")?;
    let mut parts = rest.split('-').filter(|p| !p.is_empty());
    let code = parts.next()?;
    let sub: String = parts.collect::<Vec<_>>().join("");
    if code.is_empty() || sub.is_empty() {
        return None;
    }
    if !code.chars().all(|c| c.is_ascii_digit()) || !sub.chars().all(|c| c.is_ascii_digit()) {
        return None;
    }
    Some(format!("MX-{}-{}", code, sub))
}

/// Lowercase, 3-32 chars, letters/digits/underscore/dot, must start with a letter.
pub fn normalize_username(raw: &str) -> Result<String, &'static str> {
    let u = raw.trim().trim_start_matches('@').to_lowercase();
    if u.len() < 3 {
        return Err("Username must be at least 3 characters");
    }
    if u.len() > 32 {
        return Err("Username must be at most 32 characters");
    }
    if !u.chars().next().map(|c| c.is_ascii_lowercase()).unwrap_or(false) {
        return Err("Username must start with a letter");
    }
    if !u.chars().all(|c| c.is_ascii_lowercase() || c.is_ascii_digit() || c == '_' || c == '.') {
        return Err("Only letters, numbers, underscore and dot are allowed");
    }
    if u.ends_with('.') || u.contains("..") {
        return Err("Dots cannot be at the end or repeated");
    }
    if RESERVED_USERNAMES.contains(&u.as_str()) {
        return Err("This username is reserved");
    }
    Ok(u)
}

fn random_subscriber(len: usize) -> String {
    let mut rng = rand::thread_rng();
    let mut s = String::with_capacity(len);
    s.push(char::from(b'1' + rng.gen_range(0..9u8))); // no leading zero
    for _ in 1..len {
        s.push(char::from(b'0' + rng.gen_range(0..10u8)));
    }
    s
}

fn random_token() -> String {
    let bytes: [u8; 16] = rand::thread_rng().gen();
    hex::encode(bytes)
}

#[derive(Serialize)]
pub struct NumberOffer {
    pub number: String,
    pub country: CountryInfo,
    pub offer_token: String,
    pub expires_in: u64,
}

#[derive(Serialize)]
pub struct DirectoryEntry {
    pub user_hex: String,
    pub number: String,
    pub username: String,
    pub name: String,
}

pub enum RegisterOutcome {
    Ok,
    OfferInvalid,
    NumberTaken,
    UsernameTaken,
    AlreadyRegistered,
}

const REGISTER_LUA: &str = r#"
-- KEYS: 1 hold, 2 idx:num, 3 idx:uname, 4 acct
-- ARGV: 1 token, 2 user_hex, 3 number, 4 username, 5 name, 6 country, 7 created
if redis.call('EXISTS', KEYS[4]) == 1 then return -4 end
if redis.call('GET', KEYS[1]) ~= ARGV[1] then return -1 end
if redis.call('EXISTS', KEYS[2]) == 1 then return -2 end
local owner = redis.call('GET', KEYS[3])
if owner and owner ~= ARGV[2] then return -3 end
redis.call('SET', KEYS[2], ARGV[2])
redis.call('SET', KEYS[3], ARGV[2])
redis.call('HSET', KEYS[4], 'number', ARGV[3], 'username', ARGV[4], 'name', ARGV[5], 'country', ARGV[6], 'created', ARGV[7])
redis.call('DEL', KEYS[1])
return 1
"#;

pub struct Directory {
    redis: ConnectionManager,
}

impl Directory {
    pub fn new(redis: ConnectionManager) -> Self {
        Self { redis }
    }

    /// Reserve a fresh, never-used number in the given country for OFFER_TTL_SECS.
    pub async fn offer(&self, c: CountryInfo) -> Result<Option<NumberOffer>, redis::RedisError> {
        let mut conn = self.redis.clone();
        for _ in 0..12 {
            let number = format!("MX-{}-{}", c.calling_code, random_subscriber(c.digits));
            let taken: bool = conn.exists(format!("idx:num:{}", number)).await?;
            if taken {
                continue;
            }
            let token = random_token();
            let held: Option<String> = redis::cmd("SET")
                .arg(format!("num:hold:{}", number))
                .arg(&token)
                .arg("NX")
                .arg("EX")
                .arg(OFFER_TTL_SECS)
                .query_async(&mut conn)
                .await?;
            if held.is_some() {
                return Ok(Some(NumberOffer {
                    number,
                    country: c,
                    offer_token: token,
                    expires_in: OFFER_TTL_SECS,
                }));
            }
        }
        Ok(None)
    }

    pub async fn username_available(&self, username: &str) -> Result<bool, redis::RedisError> {
        let mut conn = self.redis.clone();
        let exists: bool = conn.exists(format!("idx:uname:{}", username)).await?;
        Ok(!exists)
    }

    #[allow(clippy::too_many_arguments)]
    pub async fn register(
        &self,
        user_hex: &str,
        number: &str,
        offer_token: &str,
        username: &str,
        name: &str,
        country_iso: &str,
        created: u64,
    ) -> Result<RegisterOutcome, redis::RedisError> {
        let mut conn = self.redis.clone();
        let code: i64 = redis::Script::new(REGISTER_LUA)
            .key(format!("num:hold:{}", number))
            .key(format!("idx:num:{}", number))
            .key(format!("idx:uname:{}", username))
            .key(format!("acct:{}", user_hex))
            .arg(offer_token)
            .arg(user_hex)
            .arg(number)
            .arg(username)
            .arg(name)
            .arg(country_iso)
            .arg(created)
            .invoke_async(&mut conn)
            .await?;
        Ok(match code {
            1 => RegisterOutcome::Ok,
            -1 => RegisterOutcome::OfferInvalid,
            -2 => RegisterOutcome::NumberTaken,
            -3 => RegisterOutcome::UsernameTaken,
            _ => RegisterOutcome::AlreadyRegistered,
        })
    }

    pub async fn account(&self, user_hex: &str) -> Result<Option<DirectoryEntry>, redis::RedisError> {
        let mut conn = self.redis.clone();
        let key = format!("acct:{}", user_hex);
        let exists: bool = conn.exists(&key).await?;
        if !exists {
            return Ok(None);
        }
        let (number, username, name): (Option<String>, Option<String>, Option<String>) =
            conn.hget(&key, ("number", "username", "name")).await?;
        Ok(Some(DirectoryEntry {
            user_hex: user_hex.to_string(),
            number: number.unwrap_or_default(),
            username: username.unwrap_or_default(),
            name: name.unwrap_or_default(),
        }))
    }

    /// Resolve `@username`, `username` or `MX-<code>-<digits>` to a directory entry.
    pub async fn lookup(&self, handle: &str) -> Result<Option<DirectoryEntry>, redis::RedisError> {
        let mut conn = self.redis.clone();
        let idx_key = if let Some(num) = normalize_number(handle) {
            format!("idx:num:{}", num)
        } else {
            match normalize_username(handle) {
                Ok(u) => format!("idx:uname:{}", u),
                Err(_) => return Ok(None),
            }
        };
        let owner: Option<String> = conn.get(idx_key).await?;
        match owner {
            Some(hex) => self.account(&hex).await,
            None => Ok(None),
        }
    }
}

pub type SharedDirectory = Arc<Directory>;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn numbers_normalize() {
        assert_eq!(normalize_number("mx-234-8123456789").as_deref(), Some("MX-234-8123456789"));
        assert_eq!(normalize_number("MX 44 7123 456789").as_deref(), Some("MX-44-7123456789"));
        assert!(normalize_number("+2348123456789").is_none());
        assert!(normalize_number("MX-abc-123").is_none());
    }

    #[test]
    fn usernames_validate() {
        assert_eq!(normalize_username("@David_Caleb").unwrap(), "david_caleb");
        assert!(normalize_username("ab").is_err());
        assert!(normalize_username("1abc").is_err());
        assert!(normalize_username("mestxa").is_err());
        assert!(normalize_username("bad..name").is_err());
    }

    #[test]
    fn subscriber_has_right_length_and_no_leading_zero() {
        for len in [7usize, 8, 9, 10, 11] {
            let s = random_subscriber(len);
            assert_eq!(s.len(), len);
            assert_ne!(s.as_bytes()[0], b'0');
        }
    }

    #[test]
    fn country_lookup() {
        let ng = country("ng").unwrap();
        assert_eq!(ng.calling_code, "234");
        assert_eq!(ng.digits, 10);
        assert!(country("XX").is_none());
    }
}
