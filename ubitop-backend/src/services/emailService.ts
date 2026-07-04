import nodemailer from 'nodemailer';
import crypto from 'crypto';

const transporter = nodemailer.createTransport({
  host: 'smtp.gmail.com',
  port: 465,
  secure: true,
  auth: {
    user: process.env.SENDER_EMAIL,
    pass: process.env.SENDER_PASSWORD,
  },
});

function generateOtpCode(): string {
  const min = 100000;
  const max = 999999;
  return String(min + crypto.randomInt(0, max - min + 1));
}

function buildHtmlTemplate(otpCode: string, email: string): string {
  return `
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
</head>
<body style="margin:0;padding:0;background-color:#f4f4f7;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f7;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;">
          <tr>
            <td style="background:linear-gradient(135deg,#1a237e 0%,#5c6bc0 100%);padding:32px 40px;text-align:center;">
              <h1 style="color:#ffffff;margin:0;font-size:22px;font-weight:700;letter-spacing:0.5px;">Top Mejores Tiendas</h1>
              <p style="color:#c5cae9;margin:8px 0 0;font-size:13px;">Verificaci\u00f3n de Correo Electr\u00f3nico</p>
            </td>
          </tr>
          <tr>
            <td style="padding:40px;">
              <p style="color:#333;font-size:15px;margin:0 0 16px;line-height:1.6;">\u00a1Hola!</p>
              <p style="color:#555;font-size:14px;margin:0 0 24px;line-height:1.6;">
                Recibimos una solicitud para verificar tu correo electr\u00f3nico
                <strong style="color:#1a237e;">${email}</strong> en la aplicaci\u00f3n
                <strong>Top Mejores Tiendas</strong>. Usa el siguiente c\u00f3digo:
              </p>
              <table width="100%" cellpadding="0" cellspacing="0">
                <tr>
                  <td align="center" style="padding:16px 0;">
                    <div style="background:linear-gradient(135deg,#e8eaf6 0%,#f3e5f5 100%);border:2px dashed #5c6bc0;border-radius:12px;padding:20px 40px;display:inline-block;">
                      <span style="font-size:36px;font-weight:800;letter-spacing:12px;color:#1a237e;font-family:'Courier New',monospace;">${otpCode}</span>
                    </div>
                  </td>
                </tr>
              </table>
              <p style="color:#888;font-size:12px;margin:24px 0 0;line-height:1.5;text-align:center;">
                Este c\u00f3digo es v\u00e1lido por <strong>10 minutos</strong>.<br>
                Si no solicitaste este c\u00f3digo, puedes ignorar este mensaje.
              </p>
            </td>
          </tr>
          <tr>
            <td style="background-color:#f8f9fa;padding:20px 40px;text-align:center;border-top:1px solid #e8e8e8;">
              <p style="color:#aaa;font-size:11px;margin:0;line-height:1.5;">
                &copy; 2026 Top Mejores Tiendas &mdash; Per&uacute;<br>
                Este es un correo autom&aacute;tico, por favor no respondas.
              </p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>`.trim();
}

export async function sendVerificationCode(recipientEmail: string): Promise<string> {
  const otpCode = generateOtpCode();

  const info = await transporter.sendMail({
    from: `"Top Mejores Tiendas" <${process.env.SENDER_EMAIL}>`,
    to: recipientEmail,
    subject: 'C\u00f3digo de Verificaci\u00f3n - Top Mejores Tiendas',
    html: buildHtmlTemplate(otpCode, recipientEmail),
  });

  console.log('[EmailService] Correo enviado:', info.messageId);
  return otpCode;
}
