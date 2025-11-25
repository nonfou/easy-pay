-- V4: Add timestamp fields to pay_account table
ALTER TABLE pay_account ADD COLUMN IF NOT EXISTS created_at DATETIME DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE pay_account ADD COLUMN IF NOT EXISTS updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Update pay_channel.last_time to DATETIME if it's VARCHAR
-- Note: This may need manual adjustment based on existing data
