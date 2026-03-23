import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  console.log(`[Stream-Token] Nhận request: ${req.method} ${req.url}`);

  // 1. Xử lý CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    // 2. Chỉ chấp nhận POST
    if (req.method !== "POST") {
      console.error(`[Stream-Token] Lỗi: Method ${req.method} không được hỗ trợ`);
      return new Response("Method Not Allowed", { status: 405, headers: corsHeaders })
    }

    // 3. Lấy thông tin từ request body
    const bodyText = await req.text()
    console.log(`[Stream-Token] Request Body: ${bodyText}`);
    
    let userId: string;
    try {
      const bodyJson = JSON.parse(bodyText);
      userId = bodyJson.userId;
    } catch (e) {
      console.error(`[Stream-Token] Lỗi parse JSON body`);
      return new Response(JSON.stringify({ error: "Lỗi parse JSON payload" }), { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400 });
    }

    if (!userId || typeof userId !== "string") {
      console.error(`[Stream-Token] Lỗi: userId không hợp lệ (${userId})`);
      return new Response(
        JSON.stringify({ error: "userId là bắt buộc" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400 }
      )
    }

    console.log(`[Stream-Token] Đang Mint token cho userId: ${userId}`);

    // 4. Lấy Stream credentials từ environment variables (Supabase secrets)
    const streamApiKey = Deno.env.get("STREAM_API_KEY")
    const streamApiSecret = Deno.env.get("STREAM_API_SECRET")

    if (!streamApiKey || !streamApiSecret) {
      console.error("[Stream-Token] Lỗi: Thiếu STREAM_API_KEY hoặc STREAM_API_SECRET trong Supabase Secrets");
      throw new Error("Stream API Key hoặc Secret chưa được cấu hình. Hãy kiểm tra Supabase Dashboard -> Edge Functions -> Secrets.")
    }
    
    console.log(`[Stream-Token] Đã tìm thấy cấu hình Stream API Key: ${streamApiKey.substring(0, 5)}...`);

    // ─────────────────────────────────────────────────────────────
    // Tạo Stream JWT token bằng cách ký thủ công (không cần SDK)
    // Stream token format: HS256 JWT với claim { user_id }
    // ─────────────────────────────────────────────────────────────
    const header = { alg: "HS256", typ: "JWT" }
    const payload = {
      user_id: userId,
      // Không set exp để token không hết hạn (Stream sẽ handle session)
    }

    const base64UrlEncode = (obj: object) =>
      btoa(JSON.stringify(obj))
        .replace(/=/g, "")
        .replace(/\+/g, "-")
        .replace(/\//g, "_")

    const encodedHeader = base64UrlEncode(header)
    const encodedPayload = base64UrlEncode(payload)
    const signingInput = `${encodedHeader}.${encodedPayload}`

    // Ký bằng HMAC-SHA256
    const secretKey = await crypto.subtle.importKey(
      "raw",
      new TextEncoder().encode(streamApiSecret),
      { name: "HMAC", hash: "SHA-256" },
      false,
      ["sign"]
    )

    const signatureBuffer = await crypto.subtle.sign(
      "HMAC",
      secretKey,
      new TextEncoder().encode(signingInput)
    )

    const signature = btoa(String.fromCharCode(...new Uint8Array(signatureBuffer)))
      .replace(/=/g, "")
      .replace(/\+/g, "-")
      .replace(/\//g, "_")

    const token = `${signingInput}.${signature}`

    console.log(`[Stream-Token] => Tạo token thành công cho userId: ${userId}`);

    return new Response(
      JSON.stringify({ token }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 200
      }
    )

  } catch (error: any) {
    console.error("[Stream-Token] Lỗi hệ thống khi tạo token:", error.message);
    if (error.stack) console.error(error.stack);
    
    return new Response(
      JSON.stringify({ error: error.message }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 500
      }
    )
  }
})
