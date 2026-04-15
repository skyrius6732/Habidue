-- Add missing lastScheduleSetBy column to trade_proposals
ALTER TABLE trade_proposals
    ADD COLUMN last_schedule_set_by VARCHAR(20);
