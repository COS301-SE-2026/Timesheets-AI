-- V16__backfill_v6_burnout_member_attribution.sql
-- V7__fix_ai_insight_team_scope.sql ran `UPDATE ai_insights SET workspace_member_id
-- = NULL WHERE scope = 'TEAM'` as a blanket cleanup. That silently wiped the
-- member attribution off the 3 BURNOUT rows V6 seeded (Lubanzi, Thabang,
-- Karabo), which is why they render as "Unknown team member" on the manager
-- Insights page instead of a name.
--
-- V14 already fixed this same failure mode for 2 different orphaned rows
-- (see its section 0, "the two BURNOUT rows from V11"). This does the same
-- thing for the 3 that V14 missed, since they came from V6, not V11.
--
-- Not touching row ...903 (V14's genuine "not enough activity logged yet"
-- edge case), that one is correctly unattached, it's testing the fallback
-- path on purpose.

UPDATE ai_insights
SET workspace_member_id = '00000000-0000-0000-0006-000000000025' -- Lubanzi Gcabashe
WHERE id = '00000000-0000-0000-0001-000000000330';

UPDATE ai_insights
SET workspace_member_id = '00000000-0000-0000-0001-000000000020' -- Thabang Siduke
WHERE id = '00000000-0000-0000-0001-000000000331';

UPDATE ai_insights
SET workspace_member_id = '00000000-0000-0000-0005-000000000024' -- Karabo Mathebula
WHERE id = '00000000-0000-0000-0001-000000000332';