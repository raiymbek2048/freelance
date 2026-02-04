-- FreelanceKG Initial Categories
-- Version 2: Seed data for categories

-- Main categories
INSERT INTO categories (name, slug, description, sort_order) VALUES
    ('Разработка и IT', 'it-development', 'Веб-разработка, мобильные приложения, программирование', 1),
    ('Дизайн', 'design', 'Графический дизайн, UI/UX, иллюстрации', 2),
    ('Маркетинг и реклама', 'marketing', 'SMM, SEO, контекстная реклама, email-маркетинг', 3),
    ('Тексты и переводы', 'content', 'Копирайтинг, рерайтинг, переводы', 4),
    ('Видео и аудио', 'video-audio', 'Монтаж видео, озвучка, анимация', 5),
    ('Бизнес и финансы', 'business', 'Бухгалтерия, юридические услуги, консалтинг', 6),
    ('Ремонт и строительство', 'repair', 'Ремонт квартир, сантехника, электрика', 7),
    ('Красота и здоровье', 'beauty', 'Парикмахерские услуги, маникюр, массаж', 8),
    ('Репетиторство', 'tutoring', 'Уроки иностранных языков, подготовка к экзаменам', 9),
    ('Бытовые услуги', 'household', 'Уборка, переезды, мелкий ремонт', 10);

-- Subcategories for IT
INSERT INTO categories (name, slug, description, parent_id, sort_order)
SELECT 'Веб-разработка', 'web-development', 'Создание сайтов и веб-приложений', id, 1 FROM categories WHERE slug = 'it-development'
UNION ALL
SELECT 'Мобильная разработка', 'mobile-development', 'iOS и Android приложения', id, 2 FROM categories WHERE slug = 'it-development'
UNION ALL
SELECT 'Backend разработка', 'backend-development', 'Серверная часть, API, базы данных', id, 3 FROM categories WHERE slug = 'it-development'
UNION ALL
SELECT 'Frontend разработка', 'frontend-development', 'Интерфейсы, React, Vue, Angular', id, 4 FROM categories WHERE slug = 'it-development'
UNION ALL
SELECT '1С программирование', '1c-programming', 'Настройка и доработка 1С', id, 5 FROM categories WHERE slug = 'it-development'
UNION ALL
SELECT 'DevOps', 'devops', 'CI/CD, Docker, Kubernetes', id, 6 FROM categories WHERE slug = 'it-development';

-- Subcategories for Design
INSERT INTO categories (name, slug, description, parent_id, sort_order)
SELECT 'Логотипы и брендинг', 'logos-branding', 'Создание логотипов, фирменный стиль', id, 1 FROM categories WHERE slug = 'design'
UNION ALL
SELECT 'Веб-дизайн', 'web-design', 'Дизайн сайтов и лендингов', id, 2 FROM categories WHERE slug = 'design'
UNION ALL
SELECT 'UI/UX дизайн', 'ui-ux-design', 'Дизайн интерфейсов, прототипирование', id, 3 FROM categories WHERE slug = 'design'
UNION ALL
SELECT 'Полиграфия', 'print-design', 'Визитки, буклеты, баннеры', id, 4 FROM categories WHERE slug = 'design'
UNION ALL
SELECT 'Иллюстрации', 'illustrations', 'Рисунки, иконки, персонажи', id, 5 FROM categories WHERE slug = 'design';

-- Subcategories for Marketing
INSERT INTO categories (name, slug, description, parent_id, sort_order)
SELECT 'SMM', 'smm', 'Ведение социальных сетей', id, 1 FROM categories WHERE slug = 'marketing'
UNION ALL
SELECT 'SEO', 'seo', 'Поисковая оптимизация', id, 2 FROM categories WHERE slug = 'marketing'
UNION ALL
SELECT 'Контекстная реклама', 'ppc', 'Google Ads, Яндекс.Директ', id, 3 FROM categories WHERE slug = 'marketing'
UNION ALL
SELECT 'Таргетированная реклама', 'targeted-ads', 'Реклама в соцсетях', id, 4 FROM categories WHERE slug = 'marketing';

-- Subcategories for Content
INSERT INTO categories (name, slug, description, parent_id, sort_order)
SELECT 'Копирайтинг', 'copywriting', 'Написание текстов, статей', id, 1 FROM categories WHERE slug = 'content'
UNION ALL
SELECT 'Переводы', 'translation', 'Перевод текстов, документов', id, 2 FROM categories WHERE slug = 'content'
UNION ALL
SELECT 'Рерайтинг', 'rewriting', 'Переписывание и улучшение текстов', id, 3 FROM categories WHERE slug = 'content'
UNION ALL
SELECT 'Транскрибация', 'transcription', 'Расшифровка аудио и видео', id, 4 FROM categories WHERE slug = 'content';

-- Subcategories for Repair
INSERT INTO categories (name, slug, description, parent_id, sort_order)
SELECT 'Сантехника', 'plumbing', 'Ремонт и установка сантехники', id, 1 FROM categories WHERE slug = 'repair'
UNION ALL
SELECT 'Электрика', 'electrical', 'Электромонтажные работы', id, 2 FROM categories WHERE slug = 'repair'
UNION ALL
SELECT 'Отделочные работы', 'finishing', 'Штукатурка, покраска, обои', id, 3 FROM categories WHERE slug = 'repair'
UNION ALL
SELECT 'Мебель', 'furniture', 'Сборка и ремонт мебели', id, 4 FROM categories WHERE slug = 'repair';

-- Subcategories for Household
INSERT INTO categories (name, slug, description, parent_id, sort_order)
SELECT 'Уборка', 'cleaning', 'Уборка квартир и офисов', id, 1 FROM categories WHERE slug = 'household'
UNION ALL
SELECT 'Переезды', 'moving', 'Грузчики, перевозка вещей', id, 2 FROM categories WHERE slug = 'household'
UNION ALL
SELECT 'Курьерские услуги', 'courier', 'Доставка документов и посылок', id, 3 FROM categories WHERE slug = 'household'
UNION ALL
SELECT 'Мелкий ремонт', 'handyman', 'Мелкий бытовой ремонт', id, 4 FROM categories WHERE slug = 'household';
