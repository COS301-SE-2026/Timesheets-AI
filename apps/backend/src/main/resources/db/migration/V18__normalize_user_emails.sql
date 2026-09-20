-- Only allow emails that are lowercase and have no spaces at the beginning or end.
ALTER TABLE users
ADD CONSTRAINT chk_users_email_normalized
CHECK (email = LOWER(TRIM(email)));