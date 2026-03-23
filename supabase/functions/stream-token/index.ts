import { serve } from "https://deno.land/std@0.168.0/http/server.ts"

serve(async (req) => {
  try {
    // Chỉ chấp nhận POST
    if (req.method !== "POST") {
      return new Response("Method Not Allowed", { status: 405 })
    }

    // Lấy thông tin từ request body
    const { userId } = await req.json()

    if (!userId || typeof userId !== "string") {
      return new Response(
        JSON.stringify({ error: "userId là bắt buộc" }),
        { headers: { "Content-Type": "application/json" }, status: 400 }
      )
    }

    // Lấy Stream credentials từ environment variables (Supabase secrets)
    const streamApiKey = Deno.env.get("STREAM_API_KEY")
    const streamApiSecret = Deno.env.get("STREAM_API_SECRET")

    if (!streamApiKey || !streamApiSecret) {
      throw new Error("Stream API Key hoặc Secret chưa được cấu hình trong Supabase secrets")
    }

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

    console.log(`Stream token tạo thành công cho userId: ${userId}`)

    return new Response(
      JSON.stringify({ token }),
      {
        headers: { "Content-Type": "application/json" },
        status: 200
      }
    )

  } catch (error: any) {
    console.error("Lỗi tạo Stream token:", error.message)
    return new Response(
      JSON.stringify({ error: error.message }),
      {
        headers: { "Content-Type": "application/json" },
        status: 500
      }
    )
  }
})
