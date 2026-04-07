-- ==========================================
-- PHẦN 1: TẠO CẤU TRÚC BẢNG (TABLES)
-- ==========================================

-- 1. Bảng Users (Mở rộng từ auth.users của Supabase)
CREATE TABLE public.users (
id UUID REFERENCES auth.users(id) PRIMARY KEY,
username VARCHAR(50) NOT NULL,
display_name VARCHAR(100) NOT NULL,
email VARCHAR(255) UNIQUE NOT NULL,
bio TEXT,
phone TEXT,
avatarid TEXT NOT NULL,
avatar_url TEXT,
birthday TEXT,
gender boolean,
public_encrypt_key TEXT NOT NULL,
public_sign_key TEXT NOT NULL,
fts_search tsvector,
last_seen TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW())
);

-- 2. Bảng User_Devices (Quản lý thiết bị & Push Notification)
CREATE TABLE public.user_devices (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
fcm_token TEXT NOT NULL,
device_name VARCHAR(100),
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
UNIQUE(user_id, fcm_token)
);

-- 3. Bảng Friend_Requests (Lời mời kết bạn)
CREATE TABLE public.friend_requests (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
sender_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
receiver_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending','accepted','declined')),
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
UNIQUE(sender_id, receiver_id)
);

-- 4. Bảng Friends (Danh sách bạn bè chính thức)
CREATE TABLE public.friends (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
user_id_1 UUID REFERENCES public.users(id) ON DELETE CASCADE,
user_id_2 UUID REFERENCES public.users(id) ON DELETE CASCADE,
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
CHECK (user_id_1 < user_id_2),
UNIQUE(user_id_1, user_id_2)
);

-- 5. Bảng Conversations (Phòng Chat - Dùng chung cho 1-1 và Group)
CREATE TABLE public.conversations (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
is_group BOOLEAN DEFAULT FALSE,
name VARCHAR(100),
avatar_id TEXT,
avatar_url TEXT,
last_message_preview TEXT,
last_message_time TIMESTAMP WITH TIME ZONE,
hidden_pin_hash TEXT,
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW())
);
ALTER TABLE public.conversations ADD COLUMN last_message_sender_id UUID;

-- 6. Bảng Participants (Thành viên trong phòng chat)
CREATE TABLE public.participants (
conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
role VARCHAR(20) DEFAULT 'member',
unread_count INT DEFAULT 0,
encrypted_group_key TEXT,
status TEXT,
joined_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
PRIMARY KEY (conversation_id, user_id)
);

-- 7. Bảng Messages (Tin nhắn)
CREATE TABLE public.messages (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
sender_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
reply_to_id UUID REFERENCES public.messages(id) ON DELETE SET NULL,
encrypted_content TEXT NOT NULL,
message_type VARCHAR(20) DEFAULT 'text',
is_edited BOOLEAN DEFAULT FALSE,
seen_by UUID[] DEFAULT '{}', -- Mảng lưu UUID của những người đã xem tin nhắn này
call_duration_sec INT,
call_direction VARCHAR(10),
call_video BOOLEAN DEFAULT TRUE,
call_reason VARCHAR(20),
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE public.video_calls (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
message_id UUID NOT NULL REFERENCES public.messages(id) ON DELETE CASCADE,
duration_sec INT DEFAULT 0,
direction VARCHAR(20), -- 'incoming' hoặc 'outgoing'
is_video BOOLEAN DEFAULT TRUE,
end_reason VARCHAR(50), -- 'ended', 'missed', 'rejected', 'cancelled'
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW())
);

CREATE TABLE public.shared_tasks (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
conversation_id UUID REFERENCES public.conversations(id) ON DELETE CASCADE,
creator_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
assignee_id UUID REFERENCES public.users(id) ON DELETE SET NULL, -- Thành viên được giao nhiệm vụ
title TEXT NOT NULL,
description TEXT,
due_date TIMESTAMP WITH TIME ZONE,
is_completed BOOLEAN DEFAULT FALSE,
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW())
);

CREATE INDEX idx_videocall_message_id ON public.video_calls(message_id);


-- 8. Bảng Attachments (Tệp đính kèm)
CREATE TABLE public.attachments (
id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
message_id UUID REFERENCES public.messages(id) ON DELETE CASCADE,
file_url TEXT NOT NULL,
file_type VARCHAR(50),
file_name VARCHAR(255),
file_size INT,
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW())
);

-- 9. Bảng Message_Reactions (Thả cảm xúc)
CREATE TABLE public.message_reactions (
message_id UUID REFERENCES public.messages(id) ON DELETE CASCADE,
user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
reaction_icon VARCHAR(50) NOT NULL,
created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc'::text, NOW()),
PRIMARY KEY (message_id, user_id, reaction_icon),
count INT DEFAULT 1
);

-- ==========================================
-- PHẦN 2: TẠO CHỈ MỤC (INDEXES) TỐI ƯU HIỆU NĂNG
-- ==========================================

CREATE INDEX idx_messages_conversation_id ON public.messages(conversation_id, created_at DESC);
CREATE INDEX idx_participants_user_id ON public.participants(user_id);
CREATE INDEX idx_users_fts_search ON public.users USING GIN(fts_search);
CREATE INDEX idx_friend_requests_receiver ON public.friend_requests(receiver_id);
CREATE INDEX idx_friends_user_id_2 ON public.friends(user_id_2);
CREATE INDEX IF NOT EXISTS idx_messages_call_type ON public.messages(message_type, created_at DESC);



-- ==========================================
-- PHẦN 3: TẠO VIEWS & CÁC HÀM (RPC), TRIGGER TỰ ĐỘNG
-- ==========================================
-- 3.1 VIEW: Lấy danh sách phòng chat hiển thị cho người dùng
CREATE OR REPLACE VIEW public.chat_list_view AS
SELECT
p1.user_id AS current_user_id,            
c.id AS conversation_id,                  
c.is_group,
c.last_message_preview,
c.last_message_time,
c.last_message_sender_id,
p1.unread_count,
-- Tên và ảnh
CASE
WHEN c.is_group THEN c.name
ELSE u.display_name
END AS chat_name,
CASE
WHEN c.is_group THEN c.avatar_url
ELSE u.avatar_url
END AS chat_avatar,

    -- Trả về last_seen và id của người kia (nếu là chat 1-1)
    CASE 
        WHEN c.is_group THEN NULL 
        ELSE u.id 
    END AS target_user_id,
    CASE 
        WHEN c.is_group THEN NULL 
        ELSE u.last_seen 
    END AS target_last_seen

FROM public.participants p1
JOIN public.conversations c ON p1.conversation_id = c.id
LEFT JOIN public.participants p2 ON c.id = p2.conversation_id AND p1.user_id != p2.user_id AND c.is_group = false
LEFT JOIN public.users u ON p2.user_id = u.id;
-- 3.2 TRIGGER: Cập nhật Full-Text Search
CREATE OR REPLACE FUNCTION update_user_search_vector() RETURNS trigger AS $$
BEGIN
NEW.fts_search := to_tsvector('simple', NEW.username || ' ' || NEW.display_name || ' ' || COALESCE(NEW.phone, ''));
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER user_search_update
BEFORE INSERT OR UPDATE OF username, display_name, phone ON public.users
FOR EACH ROW EXECUTE FUNCTION update_user_search_vector();


-- 3.3 TRIGGER: Cập nhật Preview tin nhắn cuối và tăng số tin chưa đọc
CREATE OR REPLACE FUNCTION update_conversation_on_new_message() RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
-- Cập nhật tin nhắn cuối cho phòng chat
UPDATE public.conversations
SET last_message_time = NEW.created_at,
last_message_preview = NEW.encrypted_content,
last_message_sender_id = NEW.sender_id
WHERE id = NEW.conversation_id;

-- Tăng số đếm chưa đọc cho những người khác trong phòng
UPDATE public.participants
SET unread_count = unread_count + 1
WHERE conversation_id = NEW.conversation_id AND user_id != NEW.sender_id;

RETURN NEW;
END;
$$;

CREATE TRIGGER on_message_inserted
AFTER INSERT ON public.messages
FOR EACH ROW EXECUTE FUNCTION update_conversation_on_new_message();
--3.3.1 Trigger: Bất cứ khi nào thả/đổi/xóa cảm xúc, cập nhật lại updated_at của tin nhắn
CREATE OR REPLACE FUNCTION touch_message_updated_at()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
UPDATE public.messages
SET updated_at = TIMEZONE('utc'::text, NOW())
WHERE id = NEW.message_id;
RETURN NEW;
ELSIF TG_OP = 'DELETE' THEN
UPDATE public.messages
SET updated_at = TIMEZONE('utc'::text, NOW())
WHERE id = OLD.message_id;
RETURN OLD;
END IF;
RETURN NULL;
END;
$$;
-- 3.3 Gắn Trigger vào bảng message_reactions
CREATE TRIGGER on_reaction_change
AFTER INSERT OR UPDATE OR DELETE ON public.message_reactions
FOR EACH ROW EXECUTE FUNCTION touch_message_updated_at();
-- 3.4 RPC: Reset số tin chưa đọc khi User mở đoạn chat
CREATE OR REPLACE FUNCTION reset_unread_count(conv_id UUID, u_id UUID) RETURNS void AS $$
BEGIN
UPDATE public.participants
SET unread_count = 0
WHERE conversation_id = conv_id AND user_id = u_id;
END;
$$ LANGUAGE plpgsql;

-- 3.5 RPC: Chấp nhận kết bạn
CREATE OR REPLACE FUNCTION accept_friend_request(p_sender_id UUID, p_receiver_id UUID)
RETURNS BOOLEAN AS $$
DECLARE
v_user1 UUID;
v_user2 UUID;
BEGIN
UPDATE public.friend_requests
SET status = 'accepted'
WHERE sender_id = p_sender_id AND receiver_id = p_receiver_id;

    IF p_sender_id < p_receiver_id THEN
        v_user1 := p_sender_id;
        v_user2 := p_receiver_id;
    ELSE
        v_user1 := p_receiver_id;
        v_user2 := p_sender_id;
    END IF;

    INSERT INTO public.friends (user_id_1, user_id_2)
    VALUES (v_user1, v_user2)
    ON CONFLICT (user_id_1, user_id_2) DO NOTHING;

    RETURN TRUE;
END;
$$ LANGUAGE plpgsql;

-- 3.6 RPC: Lấy hoặc tạo phòng chat 1-1 (Vượt RLS bằng SECURITY DEFINER)
CREATE OR REPLACE FUNCTION get_or_create_direct_conversation(p_user_id_1 UUID, p_user_id_2 UUID)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
v_conversation_id UUID;
BEGIN
-- Tìm phòng chat 1-1 chung
SELECT c.id INTO v_conversation_id
FROM public.conversations c
JOIN public.participants p1 ON c.id = p1.conversation_id AND p1.user_id = p_user_id_1
JOIN public.participants p2 ON c.id = p2.conversation_id AND p2.user_id = p_user_id_2
WHERE c.is_group = false
LIMIT 1;

    -- Nếu có trả về luôn
    IF v_conversation_id IS NOT NULL THEN
        RETURN v_conversation_id;
    END IF;

    -- Nếu chưa, tạo phòng
    INSERT INTO public.conversations (is_group)
    VALUES (false)
    RETURNING id INTO v_conversation_id;

    -- Thêm 2 user vào phòng
    INSERT INTO public.participants (conversation_id, user_id, role)
    VALUES 
        (v_conversation_id, p_user_id_1, 'member'),
        (v_conversation_id, p_user_id_2, 'member');

    RETURN v_conversation_id;
END;
$$;
--3.7 chức năng lưu danh sách những người đã xem tin nhắn
CREATE OR REPLACE FUNCTION mark_message_seen(p_message_id UUID, p_user_id UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
-- Cập nhật mảng seen_by: Thêm ID người dùng vào mảng nếu ID đó chưa tồn tại trong mảng
UPDATE public.messages
SET seen_by = array_append(seen_by, p_user_id)
WHERE id = p_message_id
AND NOT (p_user_id = ANY(seen_by));
END;
$$;
-- 3.8 Tạo hàm RPC (Stored Procedure) để xử lý logic cộng dồn hoặc ghi đè
CREATE OR REPLACE FUNCTION add_message_reaction(p_message_id UUID, p_user_id UUID, p_icon VARCHAR)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
-- Kiểm tra xem user này ĐÃ THẢ ICON NÀY chưa?
IF EXISTS (
SELECT 1 FROM public.message_reactions
WHERE message_id = p_message_id
AND user_id = p_user_id
AND reaction_icon = p_icon
) THEN
-- Nếu đã có sẵn icon này rồi -> XÓA (Thu hồi)
DELETE FROM public.message_reactions
WHERE message_id = p_message_id
AND user_id = p_user_id
AND reaction_icon = p_icon;
ELSE
-- Nếu chưa có icon này -> THÊM MỚI
INSERT INTO public.message_reactions (message_id, user_id, reaction_icon, count)
VALUES (p_message_id, p_user_id, p_icon, 1);
END IF;
END;
$$;
--3.9 cập nhật từ chối lời mời
CREATE OR REPLACE FUNCTION upsert_friend_request(p_sender_id UUID, p_receiver_id UUID)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
-- 1. Xóa tất cả các lịch sử cũ (bất kể chiều nào) để dọn rác
DELETE FROM public.friend_requests
WHERE (sender_id = p_sender_id AND receiver_id = p_receiver_id)
OR (sender_id = p_receiver_id AND receiver_id = p_sender_id);

    -- 2. Insert bản ghi mới với trạng thái pending và thời gian hiện tại
    INSERT INTO public.friend_requests (sender_id, receiver_id, status, created_at)
    VALUES (p_sender_id, p_receiver_id, 'pending', TIMEZONE('utc'::text, NOW()));
END;
$$;
-- 3.10 Tạo RPC cập nhật thời gian hoạt động cuối của user hiện tại
CREATE OR REPLACE FUNCTION update_last_seen()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
UPDATE public.users
SET last_seen = TIMEZONE('utc'::text, NOW())
WHERE id = auth.uid(); -- Tự động lấy ID của người đang đăng nhập
END;
$$;
---3.11
-- Hàm Upsert (Thêm hoặc Cập nhật) FCM Token
CREATE FUNCTION public.upsert_fcm_token(p_token TEXT, p_device_name TEXT)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
-- Chỉ chạy nếu user đã đăng nhập
IF auth.uid() IS NULL THEN
RAISE EXCEPTION 'Not authenticated';
END IF;

    -- Thử chèn dữ liệu mới
    INSERT INTO public.user_devices (user_id, fcm_token, device_name, created_at, updated_at)
    VALUES (auth.uid(), p_token, p_device_name, TIMEZONE('utc'::text, NOW()), TIMEZONE('utc'::text, NOW()))
    
    -- Nếu bị trùng (User_ID và Device_Name đã tồn tại) -> Cập nhật Token mới
    ON CONFLICT (user_id, device_name) 
    DO UPDATE SET 
        fcm_token = EXCLUDED.fcm_token,
        updated_at = TIMEZONE('utc'::text, NOW());
END;
$$;

-- Permit system-style call messages (sender may be NULL for system)
DO $$
BEGIN
IF NOT EXISTS (
SELECT 1
FROM pg_policies
WHERE tablename = 'messages'
AND policyname = 'Allow system call messages'
) THEN
CREATE POLICY "Allow system call messages"
ON public.messages FOR INSERT
TO authenticated
WITH CHECK (
(sender_id = auth.uid() OR sender_id IS NULL)
AND message_type IN ('CALL_MISSED','CALL_ENDED','CALL_CANCELLED','CALL_STARTED')
);
END IF;
END$$;
--3.13. Create Group
CREATE OR REPLACE FUNCTION create_group_conversation(
p_name TEXT,
p_avatar_url TEXT,
p_user_ids JSONB,
p_encrypted_keys JSONB
)
RETURNS SETOF public.chat_list_view
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
v_conversation_id UUID;
v_user_id TEXT;
v_creator_id UUID := auth.uid();
BEGIN
IF v_creator_id IS NULL THEN
RAISE EXCEPTION 'Chưa đăng nhập';
END IF;

    -- 1. Tạo bản ghi cuộc hội thoại
    INSERT INTO public.conversations (is_group, name, avatar_url)
    VALUES (true, p_name, p_avatar_url)
    RETURNING id INTO v_conversation_id;

    -- 2. Thêm người tạo với vai trò 'admin'
    INSERT INTO public.participants (conversation_id, user_id, role, encrypted_group_key)
    VALUES (v_conversation_id, v_creator_id, 'admin', (p_encrypted_keys->>v_creator_id::TEXT));

    -- 3. Thêm các người tham gia khác (tránh trường hợp người tạo bị lặp lại trong danh sách)
    FOR v_user_id IN SELECT jsonb_array_elements_text(p_user_ids)
    LOOP
        IF v_user_id::UUID != v_creator_id THEN
            INSERT INTO public.participants (conversation_id, user_id, role, encrypted_group_key)
            VALUES (v_conversation_id, v_user_id::UUID, 'member', (p_encrypted_keys->>v_user_id));
        END IF;
    END LOOP;

    -- 4. Trả về kết quả
    RETURN QUERY
    SELECT * FROM public.chat_list_view
    WHERE conversation_id = v_conversation_id AND current_user_id = v_creator_id;
END;
$$;
-- ==========================================
-- PHẦN 4: THIẾT LẬP BẢO MẬT RLS (ROW LEVEL SECURITY)
-- ==========================================

-- Bật RLS
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.participants ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.message_reactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friend_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.friends ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.attachments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_tasks ENABLE ROW LEVEL SECURITY;


ALTER TABLE public.user_devices ADD CONSTRAINT unique_user_device UNIQUE (user_id, device_name);

-- --- POLICY CHO BẢNG USERS ---
CREATE POLICY "Cho phép xem profile" ON public.users FOR SELECT USING (true);
CREATE POLICY "Cho phép user tự tạo profile" ON public.users FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "Cho phép user tự sửa profile" ON public.users FOR UPDATE USING (auth.uid() = id);

-- --- POLICY CHO BẢNG CONVERSATIONS ---
CREATE POLICY "Users can view their own conversations"
ON public.conversations FOR SELECT
USING (
EXISTS (
SELECT 1 FROM public.participants
WHERE conversation_id = conversations.id
AND user_id = auth.uid()
)
);

CREATE POLICY "Users can create conversations"
ON public.conversations FOR INSERT
TO authenticated
WITH CHECK (true);

-- --- POLICY CHO BẢNG PARTICIPANTS ---
-- 2. Tạo một hàm kiểm tra quyền thành viên (SECURITY DEFINER giúp bỏ qua RLS của bảng đó khi kiểm tra)
CREATE OR REPLACE FUNCTION public.is_member_of(p_conversation_id UUID)
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
STABLE
AS $$
SELECT EXISTS (
SELECT 1 FROM public.participants
WHERE conversation_id = p_conversation_id
AND user_id = auth.uid()
);
$$;
-- 3. Tạo lại policy mới sử dụng hàm trung gian
CREATE POLICY "Users can view participants in shared conversations"
ON public.participants FOR SELECT
TO authenticated
USING (
is_member_of(conversation_id)
);

CREATE POLICY "Users can add participants"
ON public.participants FOR INSERT
TO authenticated
WITH CHECK (true);

CREATE POLICY "Users can update their own participant info"
ON public.participants FOR UPDATE
TO authenticated
USING (user_id = auth.uid());

-- --- POLICY CHO BẢNG MESSAGES ---
CREATE POLICY "Users can view messages in their conversations"
ON public.messages FOR SELECT
USING (
EXISTS (
SELECT 1 FROM public.participants
WHERE conversation_id = messages.conversation_id
AND user_id = auth.uid()
)
);

-- Dùng IN thay cho EXISTS tránh báo lỗi alias khi gửi tin nhắn
CREATE POLICY "User can send message"
ON public.messages FOR INSERT
TO authenticated
WITH CHECK (
sender_id = auth.uid() AND
conversation_id IN (
SELECT conversation_id
FROM public.participants
WHERE user_id = auth.uid()
)
);

-- --- POLICY CHO STORAGE BUCKET (AVATARS) ---
CREATE POLICY "Public Access"
ON storage.objects FOR SELECT
USING ( bucket_id = 'avatars' );

CREATE POLICY "Auth Insert"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK ( bucket_id = 'avatars' );

-- Chính sách: Chỉ thành viên trong phòng mới được xem và quản lý task của phòng đó
CREATE POLICY "Users can view and manage tasks in their conversations"
ON public.shared_tasks FOR ALL
TO authenticated
USING (
conversation_id IN (
SELECT conversation_id FROM public.participants WHERE user_id = auth.uid()
)
);


CREATE POLICY "Allow reading all reactions"
ON public.message_reactions FOR SELECT USING (true);

CREATE POLICY "Allows you to add reactions user"
ON public.message_reactions FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Allow users to update reactions themselves"
ON public.message_reactions FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Allows users to revoke reactions themselves"
ON public.message_reactions FOR DELETE USING (auth.uid() = user_id);


--  Ép gửi toàn bộ dữ liệu đi (Tránh bẫy im re khi Update/Delete)
ALTER TABLE public.friend_requests REPLICA IDENTITY FULL;
ALTER TABLE public.friends REPLICA IDENTITY FULL;

-- Cho phép Realtime gửi dữ liệu về đúng máy của người gửi hoặc nhận
CREATE POLICY "Cho phép xem friend_requests" ON public.friend_requests
FOR SELECT USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

CREATE POLICY "Cho phép xem friends" ON public.friends
FOR SELECT USING (auth.uid() = user_id_1 OR auth.uid() = user_id_2);

-- INSERT: Chỉ người gửi (sender) mới được quyền tạo lời mời kết bạn
CREATE POLICY "Allow you to create friend_requests"
ON public.friend_requests
FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = sender_id);

-- UPDATE: Cả người gửi (nếu muốn đổi ý) và người nhận (chấp nhận/từ chối) đều được quyền sửa
CREATE POLICY "Allow you to update friend_requests"
ON public.friend_requests
FOR UPDATE
TO authenticated
USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

-- DELETE: Cả người gửi và người nhận đều có quyền xóa/thu hồi lời mời
CREATE POLICY "Allow you to delete friend_requests"
ON public.friend_requests
FOR DELETE
TO authenticated
USING (auth.uid() = sender_id OR auth.uid() = receiver_id);

-- INSERT: Chỉ được phép thêm vào danh sách bạn bè nếu ID của mình nằm trong cột 1 hoặc cột 2
CREATE POLICY "Allow you to create links"
ON public.friends
FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = user_id_1 OR auth.uid() = user_id_2);

-- UPDATE: Chỉ 2 người trong mối quan hệ bạn bè mới được quyền cập nhật (Dự phòng cho sau này)
CREATE POLICY "Allow you to update friends"
ON public.friends
FOR UPDATE
TO authenticated
USING (auth.uid() = user_id_1 OR auth.uid() = user_id_2);

-- DELETE: Chỉ 1 trong 2 người mới có quyền Hủy kết bạn (Unfriend)
CREATE POLICY "Allow you to delete friends"
ON public.friends
FOR DELETE
TO authenticated
USING (auth.uid() = user_id_1 OR auth.uid() = user_id_2);

CREATE POLICY "Allow authenticated insert attachments"
ON public.attachments FOR INSERT
TO authenticated WITH CHECK (true);

CREATE POLICY "Allow all select attachments"
ON public.attachments FOR SELECT USING (true);