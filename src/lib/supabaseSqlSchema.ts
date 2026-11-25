export const SUPABASE_SQL_SCRIPT = `-- =====================================================================
-- تحدي طريق المزود - صوم الميلاد المجيد
-- اجتماع أبو سيفين للشباب الخريجين بالشارقة
-- الصق هذا الكود بالكامل في Supabase SQL Editor ثم اضغط Run
-- =====================================================================

-- 1) جدول المشاركين (users)
CREATE TABLE IF NOT EXISTS public.users (
  id TEXT PRIMARY KEY,
  full_name TEXT NOT NULL,
  phone TEXT NOT NULL DEFAULT '',
  pin_hash TEXT NOT NULL,
  bonus_points INTEGER NOT NULL DEFAULT 0,
  created_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

CREATE INDEX IF NOT EXISTS idx_users_full_name ON public.users (LOWER(full_name));

-- 2) جدول المهام الروحية (tasks)
CREATE TABLE IF NOT EXISTS public.tasks (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  points INTEGER NOT NULL DEFAULT 100 CHECK (points >= 0),
  frequency TEXT NOT NULL CHECK (frequency IN ('daily', 'weekly', 'once')),
  icon_key TEXT NOT NULL DEFAULT 'star',
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INTEGER NOT NULL DEFAULT 0
);

-- 3) جدول تسجيل الإنجازات والموافقات (submissions)
CREATE TABLE IF NOT EXISTS public.submissions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  user_name TEXT NOT NULL,
  task_id TEXT NOT NULL REFERENCES public.tasks(id) ON DELETE CASCADE,
  task_title TEXT NOT NULL,
  points INTEGER NOT NULL DEFAULT 100 CHECK (points >= 0),
  period_key TEXT NOT NULL,
  submission_date TEXT NOT NULL,
  note TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'approved', 'rejected')),
  admin_note TEXT NOT NULL DEFAULT '',
  submitted_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT,
  reviewed_at BIGINT NULL
);

CREATE INDEX IF NOT EXISTS idx_submissions_user_id ON public.submissions (user_id);
CREATE INDEX IF NOT EXISTS idx_submissions_status ON public.submissions (status);
CREATE UNIQUE INDEX IF NOT EXISTS idx_submissions_unique_period
  ON public.submissions (user_id, task_id, period_key)
  WHERE status IN ('pending', 'approved');

-- 4) جدول إعدادات المسابقة الأسبوعية والآية اليومية (app_config)
CREATE TABLE IF NOT EXISTS public.app_config (
  id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  weekly_quiz_title TEXT NOT NULL DEFAULT 'مسابقة نهاية الأسبوع',
  weekly_quiz_question TEXT NOT NULL DEFAULT '',
  weekly_quiz_url TEXT NOT NULL DEFAULT '',
  daily_verse_text TEXT NOT NULL DEFAULT '«المجد لله في الأعالي، وعلى الأرض السلام، وبالناس المسرة»',
  daily_verse_ref TEXT NOT NULL DEFAULT 'إنجيل لوقا 2: 14',
  daily_encouragement TEXT NOT NULL DEFAULT 'كل خطوة في طريق المزود تقربنا لملك السلام.'
);

-- تفعيل Row Level Security (RLS)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.app_config ENABLE ROW LEVEL SECURITY;

-- سياسات الوصول لتشغيل التطبيق مباشرة عبر anon key
DROP POLICY IF EXISTS "allow_app_users_all" ON public.users;
CREATE POLICY "allow_app_users_all" ON public.users
  FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "allow_app_tasks_all" ON public.tasks;
CREATE POLICY "allow_app_tasks_all" ON public.tasks
  FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "allow_app_submissions_all" ON public.submissions;
CREATE POLICY "allow_app_submissions_all" ON public.submissions
  FOR ALL USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "allow_app_config_all" ON public.app_config;
CREATE POLICY "allow_app_config_all" ON public.app_config
  FOR ALL USING (true) WITH CHECK (true);

-- إدخال المهام الـ 9 الأساسية لتحدي طريق المزود
INSERT INTO public.tasks (id, title, description, points, frequency, icon_key, is_active, sort_order) VALUES
  ('task_bible_reading', 'قراءة الإنجيل اليومية', 'قراءة أصحاح يومي من الكتاب المقدس بتأمل', 100, 'daily', 'book', TRUE, 1),
  ('task_weekly_quiz', 'مسابقة نهاية الأسبوع', 'المشاركة والإجابة في المسابقة الأسبوعية للاجتماع', 200, 'weekly', 'quiz', TRUE, 2),
  ('task_confession', 'سر الاعتراف', 'ممارسة سر التوبة والاعتراف خلال صوم الميلاد', 200, 'once', 'confession', TRUE, 3),
  ('task_liturgy', 'حضور القداس والتناول', 'حضور القداس الإلهي والتناول من الأسرار المقدسة', 100, 'weekly', 'church', TRUE, 4),
  ('task_kiahk_tasbeha', 'حضور تسبحة كيهك', 'المشاركة في سهرة التسبحة الكيهكية الأسبوعية', 150, 'weekly', 'music', TRUE, 5),
  ('task_meeting', 'حضور الاجتماع أو التلمذة', 'حضور اجتماع أبو سيفين للشباب الخريجين أو لقاء التلمذة', 100, 'weekly', 'groups', TRUE, 6),
  ('task_agpeya', 'صلاة الأجبية (ساعة على الأقل)', 'صلاة ساعة على الأقل يومياً من كتاب الأجبية المقدسة', 100, 'daily', 'prayer', TRUE, 7),
  ('task_jesus_prayer', 'صلاة يسوع', 'ترديد صلاة يسوع بتركيز وخشوع يومياً', 100, 'daily', 'heart', TRUE, 8),
  ('task_fasting_metanoias', 'الصوم الانقطاعي والميطانيات', 'الالتزام بالصوم الانقطاعي والميطانيات حسب إرشاد أب الاعتراف', 100, 'daily', 'flame', TRUE, 9)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.app_config (id) VALUES (1) ON CONFLICT (id) DO NOTHING;

GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON TABLE public.users, public.tasks, public.submissions, public.app_config TO anon, authenticated;

-- 5) كلمة مرور الأدمن في قاعدة البيانات (مشفّرة بـ bcrypt وممنوع قراءتها أو تعديلها مباشرة)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS public.admin_secrets (
  id INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
  pin_hash TEXT NOT NULL,
  updated_at BIGINT NOT NULL DEFAULT (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
);

ALTER TABLE public.admin_secrets ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.admin_secrets FROM PUBLIC, anon, authenticated;

SET search_path TO public, extensions;
INSERT INTO public.admin_secrets (id, pin_hash)
VALUES (1, crypt('2026', gen_salt('bf', 10)))
ON CONFLICT (id) DO NOTHING;

CREATE OR REPLACE FUNCTION public.verify_admin_pin(input_pin TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions
AS $$
DECLARE
  stored_hash TEXT;
BEGIN
  IF input_pin IS NULL OR length(trim(input_pin)) = 0 THEN
    RETURN FALSE;
  END IF;
  SELECT pin_hash INTO stored_hash FROM public.admin_secrets WHERE id = 1;
  IF stored_hash IS NULL THEN
    RETURN FALSE;
  END IF;
  RETURN (crypt(trim(input_pin), stored_hash) = stored_hash);
END;
$$;

CREATE OR REPLACE FUNCTION public.update_admin_pin(current_pin TEXT, new_pin TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, extensions
AS $$
DECLARE
  stored_hash TEXT;
BEGIN
  IF current_pin IS NULL OR new_pin IS NULL OR length(trim(new_pin)) < 4 THEN
    RETURN FALSE;
  END IF;
  SELECT pin_hash INTO stored_hash FROM public.admin_secrets WHERE id = 1;
  IF stored_hash IS NULL THEN
    RETURN FALSE;
  END IF;
  IF crypt(trim(current_pin), stored_hash) <> stored_hash THEN
    RETURN FALSE;
  END IF;
  UPDATE public.admin_secrets
  SET pin_hash = crypt(trim(new_pin), gen_salt('bf', 10)),
      updated_at = (EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT
  WHERE id = 1;
  RETURN TRUE;
END;
$$;

REVOKE ALL ON FUNCTION public.verify_admin_pin(TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.update_admin_pin(TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.verify_admin_pin(TEXT) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.update_admin_pin(TEXT, TEXT) TO anon, authenticated;

NOTIFY pgrst, 'reload schema';`;
