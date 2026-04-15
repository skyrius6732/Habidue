-- Drop the deprecated trade_schedules table
-- All schedule data is now stored as JSON in TradeProposal table
DROP TABLE IF EXISTS trade_schedules;
