import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import admin from "npm:firebase-admin@11.11.0"

serve(async (req) => {
  try {
    // 1. KHỞI TẠO FIREBASE NGAY TẠI ĐÂY (Bắt buộc)
    if (!admin.apps.length) {
      const serviceAccountRaw = Deno.env.get('FIREBASE_SERVICE_ACCOUNT');

      // Kiểm tra xem chìa khóa có tồn tại không
      if (!serviceAccountRaw) {
        throw new Error("LỖI: Không tìm thấy biến môi trường FIREBASE_SERVICE_ACCOUNT trên máy chủ!");
      }

      // Xóa bỏ dấu nháy đơn (') thừa ở đầu và cuối chuỗi nếu bị dính từ file .env
      const cleanServiceAccountRaw = serviceAccountRaw.replace(/^'|'$/g, '').trim();

      const serviceAccount = JSON.parse(cleanServiceAccountRaw);
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      });
      console.log("Khởi tạo Firebase Admin thành công!");
    }

    // 2. Lấy dữ liệu Webhook gửi tới (Tin nhắn mới)
    const payload = await req.json()
    const record = payload.record

    const senderId = record.sender_id
    const conversationId = record.conversation_id

    if (!senderId) return new Response('Ignored system message', { status: 200 })

    // 3. Khởi tạo Supabase Admin Client
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // 4. Lấy tên của người gửi
    const { data: sender } = await supabaseAdmin
      .from('users')
      .select('display_name')
      .eq('id', senderId)
      .single()

    // 5. Lấy ID những người trong phòng (Trừ người gửi)
    const { data: participants } = await supabaseAdmin
      .from('participants')
      .select('user_id')
      .eq('conversation_id', conversationId)
      .neq('user_id', senderId)

    if (!participants || participants.length === 0) {
      return new Response('No receivers found', { status: 200 })
    }

    const receiverIds = participants.map((p: any) => p.user_id)

    // 6. Lấy FCM Token từ bảng user_devices
    const { data: devices } = await supabaseAdmin
      .from('user_devices')
      .select('fcm_token')
      .in('user_id', receiverIds)

    if (!devices || devices.length === 0) {
      return new Response('No tokens found for receivers', { status: 200 })
    }

    const tokens = devices.map((d: any) => d.fcm_token)

    // 7. Gửi Data Payload sang Firebase
    const message = {
      data: {
        type: "NEW_MESSAGE",
        conversationId: conversationId,
        senderId: senderId,
        senderName: sender?.display_name || "Người dùng",
      },
      tokens: tokens,
    }

    const response = await admin.messaging().sendEachForMulticast(message)
    console.log("Firebase gửi thành công:", response)

    return new Response(JSON.stringify(response), {
      headers: { "Content-Type": "application/json" },
      status: 200
    })

  } catch (error: any) {
    console.error("LỖI NẶNG:", error.message)
    return new Response(JSON.stringify({ error: error.message }), {
      headers: { "Content-Type": "application/json" },
      status: 500
    })
  }
})