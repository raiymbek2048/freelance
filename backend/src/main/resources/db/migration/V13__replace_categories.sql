-- V13: Replace old categories with 6 new flat categories

-- 1. Remove executor-category links for old categories
DELETE FROM executor_categories;

-- 2. Deactivate all old subcategories (children first due to FK)
UPDATE categories SET active = false WHERE parent_id IS NOT NULL;

-- 3. Deactivate all old root categories
UPDATE categories SET active = false WHERE parent_id IS NULL;

-- 4. Insert 6 new flat categories
INSERT INTO categories (name, slug, description, sort_order, active) VALUES
    ('Дом и ремонт', 'dom-i-remont', 'Ремонт квартир, сантехника, электрика, мебель, отделка', 1, true),
    ('Услуги и помощь на выезде', 'uslugi-na-vyezde', 'Переезды, доставка, помощь мастеру, демонтаж, физическая помощь', 2, true),
    ('Уборка и бытовая помощь', 'uborka-i-byt', 'Уборка квартир, офисов, генеральная уборка, химчистка', 3, true),
    ('Онлайн-услуги', 'online-uslugi', 'Разработка, дизайн, тексты, переводы, видеомонтаж, репетиторство', 4, true),
    ('Маркетинг и продвижение', 'marketing-i-prodvizhenie', 'SMM, SEO, реклама, таргет, продвижение бизнеса', 5, true),
    ('Другое', 'drugoe', 'Задачи, не вошедшие в основные категории', 6, true);

-- 5. Move existing orders to "Другое" category
UPDATE orders SET category_id = (SELECT id FROM categories WHERE slug = 'drugoe')
WHERE category_id IN (SELECT id FROM categories WHERE active = false);
