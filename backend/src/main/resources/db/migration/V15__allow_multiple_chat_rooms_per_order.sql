-- Allow multiple chat rooms per order (one per executor)
-- Previously: UNIQUE(order_id) — only one chat room per order
-- Now: UNIQUE(order_id, executor_id) — one chat room per order+executor pair

ALTER TABLE chat_rooms DROP CONSTRAINT chat_rooms_order_id_key;
ALTER TABLE chat_rooms ADD CONSTRAINT chat_rooms_order_executor_unique UNIQUE (order_id, executor_id);
