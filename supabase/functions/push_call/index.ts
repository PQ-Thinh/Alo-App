import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"
import admin from "npm:firebase-admin@11.11.0"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // CORS Preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    if (req.method !== "POST") {
      return new Response("Method Not Allowed", { status: 405, headers: corsHeaders })
    }

    // 1. Khởi tạo Firebase
    if (!admin.apps.length) {
      const serviceAccountRaw = Deno.env.get('FIREBASE_SERVICE_ACCOUNT')
      if (!serviceAccountRaw) {
        throw new Error("LỖI: Không tìm thấy biến môi trường FIREBASE_SERVICE_ACCOUNT")
      }
      const cleanServiceAccountRaw = serviceAccountRaw.replace(/^'|'$/g, '').trim()
      const serviceAccount = JSON.parse(cleanServiceAccountRaw)
      admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
      })
    }

    // 2. Parse Body từ client Android
    const payload = await req.json()
    console.log("[push-call] 1. Nhận Payload từ Android Client:", JSON.stringify(payload))
    const { senderId, receiverIds, callId } = payload

    if (!senderId || !receiverIds || !callId || receiverIds.length === 0) {
      return new Response(
        JSON.stringify({ error: "Thiếu tham số bắt buộc: senderId, receiverIds, callId" }), 
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // 3. Khởi tạo Supabase Admin
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    // 4. Lấy thông tin người gọi để hiển thị trên thông báo máy người nhận
    const { data: sender } = await supabaseAdmin
      .from('users')
      .select('display_name, avatar_url')
      .eq('id', senderId)
      .single()

    // 5. Lấy FCM Token của những người nhận
    console.log(`[push-call] 3. Đang tra cứu FCM token trong user_devices cho danh sách id:`, receiverIds)
    const { data: devices, error: deviceError } = await supabaseAdmin
      .from('user_devices')
      .select('fcm_token')
      .in('user_id', receiverIds)

    if (deviceError) {
      console.error("[push-call] Lỗi truy vấn bảng user_devices:", deviceError)
    }

    if (!devices || devices.length === 0) {
       console.log(`[push-call] Không tìm thấy FCM Token nào cho ${receiverIds}`)
       return new Response(JSON.stringify({ message: 'Không có thiết bị nhận' }), {
         status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" }
       })
    }

    const tokens = devices.map((d: any) => d.fcm_token)
    console.log(`[push-call] 4. Đã tìm thấy ${tokens.length} token:`, tokens)

    // 6. Gửi Firebase FCM với type = INCOMING_CALL (Khớp với MyFirebaseMessagingService bên Android)
    const message = {
      data: {
        type: "INCOMING_CALL",
        senderName: sender?.display_name || "Người gọi",
        senderAvatar: sender?.avatar_url || "",
        callId: callId
      },
      // Ưu tiên cao để đánh thức máy ngay lập tức (Rất quan trọng cho tính năng Call)
      android: {
        priority: "high" as const,
      },
      tokens: tokens,
    }

    const response = await admin.messaging().sendEachForMulticast(message)
    console.log(`[push-call] 5. Kết quả gửi FCM tới ${tokens.length} thiết bị:`, JSON.stringify(response))

    return new Response(JSON.stringify({ success: true, firebaseResponse: response }), {
      status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" }
    })

  } catch (error: any) {
    console.error("[push-call] LỖI:", error.message)
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" }
    })
  }
})
