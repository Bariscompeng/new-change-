// routes/authRoutes.js
const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');
const db = require('../models');
const nodemailer = require('nodemailer'); // mail işlemi için
require('dotenv').config();

const JWT_SECRET = process.env.JWT_SECRET || 'dev_secret';

// Token oluşturma fonksiyonu
function signToken(user) {
  return jwt.sign(
    { sub: user.id, email: user.email },
    JWT_SECRET,
    { expiresIn: '7d' }
  );
}

// Mail ayarları
const transporter = nodemailer.createTransport({
    host: process.env.MAIL_HOST,
    port: process.env.MAIL_PORT,
    secure: false,
    auth: {
        user: process.env.MAIL_USER,
        pass: process.env.MAIL_PASS
    }
});

// Mail gönderme fonksiyonu
async function sendVerificationMail(toEmail, name, code) {
  const html = `
    <h2>Merhaba ${name}</h2>
    <p>Kayıt işleminiz başarıyla tamamlandı!</p>
    <p>Hesabınızı doğrulamak için aşağıdaki 4 haneli kodu girin:</p>
    <h3>${code}</h3>
    <p>Bu kod 24 saat geçerlidir.</p>
  `;
  
  try {
    await transporter.sendMail({
      from: `"Exam HUB" <${process.env.MAIL_USER}>`,
      to: toEmail,
      subject: "Hesabınızı doğrulayın",
      html
    });
  } catch (err) {
    console.error("Mail gönderme hatası:", err.message);
  }
}

function generateVerificationCode() {
  return Math.floor(1000 + Math.random() * 9000).toString(); // 4 haneli string
}
// POST /api/auth/register
router.post('/auth/register', async (req, res) => {
  const { name, email, password } = req.body || {};
  if (!name || !email || !password) {
    return res.status(400).json({ message: 'name, email, password zorunlu' });
  }

  const exists = await db.User.findOne({ where: { email } });
  if (exists) return res.status(409).json({ message: 'Bu e-posta zaten kayıtlı' });

  const passwordHash = await bcrypt.hash(password, 10);

  const verificationCode = generateVerificationCode();
  const codeExpiresAt = new Date(Date.now() + 24*60*60*1000); // 24 saat geçerli

  const user = await db.User.create({
    name,
    email,
    passwordHash,
    isVerified: false,
    verificationCode,
    codeExpiresAt
  });

  sendVerificationMail(email, name, verificationCode); // Mailde kod gönderilecek

  return res.json({ message: 'Kayıt başarılı. Mailine gelen kodu doğrula.', userId: user.id });
});

// POST /api/auth/login
router.post('/auth/login', async (req, res) => {
  try {
    const { email, password } = req.body || {};
    if (!email || !password) {
      return res.status(400).json({ message: 'email ve password zorunlu' });
    }

    const user = await db.User.findOne({ where: { email } });
    if (!user) return res.status(401).json({ message: 'Geçersiz kimlik bilgileri' });

    const ok = await bcrypt.compare(password, user.passwordHash);
    if (!ok) return res.status(401).json({ message: 'Geçersiz kimlik bilgileri' });

    const token = signToken(user);
    console.log('🔑 Login:', { id: user.id, email: user.email });
    return res.json({ token, user: { id: String(user.id), name: user.name, email: user.email, isVerified: user.isVerified } });
  } catch (e) {
    console.error('login hata:', e);
    return res.status(500).json({ message: 'Sunucu hatası' });
  }
});

// GET /api/auth/me
router.get('/auth/me', async (req, res) => {
  try {
    const auth = req.headers.authorization || '';
    const token = auth.startsWith('Bearer ') ? auth.slice(7) : null;
    if (!token) return res.status(401).json({ message: 'Yetkisiz' });

    const payload = jwt.verify(token, JWT_SECRET);
    const user = await db.User.findByPk(payload.sub);
    if (!user) return res.status(401).json({ message: 'Yetkisiz' });

    return res.json({ id: String(user.id), name: user.name, email: user.email, isVerified: user.isVerified });
  } catch (e) {
    return res.status(401).json({ message: 'Yetkisiz' });
  }
});

// GET /api/auth/verify?token=...
router.get('/auth/verify', async (req, res) => {
  const { token } = req.query;
  try {
    const payload = jwt.verify(token, JWT_SECRET);
    const user = await db.User.findByPk(payload.sub);
    if (!user) return res.status(404).send("Kullanıcı bulunamadı");

    user.isVerified = true;
    await user.save();

    res.send("📧 Mailiniz doğrulandı!");
  } catch (e) {
    console.error("Mail doğrulama hatası:", e.message);
    res.status(400).send("Geçersiz veya süresi dolmuş token");
  }
});

router.post('/auth/verify-code', async (req, res) => {
  const { email, code } = req.body || {};
  if (!email || !code) return res.status(400).json({ message: 'email ve code gerekli' });

  const user = await db.User.findOne({ where: { email } });
  if (!user) return res.status(404).json({ message: 'Kullanıcı bulunamadı' });

  if (user.isVerified) return res.json({ message: 'Zaten doğrulanmış' });

  if (user.verificationCode !== code) return res.status(400).json({ message: 'Kod yanlış' });
  if (new Date() > user.codeExpiresAt) return res.status(400).json({ message: 'Kodun süresi dolmuş' });

  user.isVerified = true;
  user.verificationCode = null;
  user.codeExpiresAt = null;
  await user.save();

  res.json({ message: '📧 Mailiniz doğrulandı!' });
});

//şifremi unuttum
router.post('/auth/forgot-password', async (req, res) => {
  console.log("🔔 /auth/forgot-password çağrıldı", req.body);
  const { email } = req.body;
  const user = await db.User.findOne({ where: { email } });
  if (!user) return res.status(404).json({ message: 'Kullanıcı bulunamadı' });

  const code = Math.floor(1000 + Math.random() * 9000).toString(); // 4 haneli
  const expiresAt = new Date(Date.now() + 15 * 60 * 1000); // 15 dk geçerli

  user.verificationCode = code;
  user.codeExpiresAt = expiresAt;
  await user.save();

  // Mail gönder
  await transporter.sendMail({
    from: `"Exam HUB" <${process.env.MAIL_USER}>`,
    to: email,
    subject: "Şifre sıfırlama kodunuz",
    html: `<p>Şifre sıfırlama kodunuz: <b>${code}</b></p><p>Bu kod 15 dakika geçerlidir.</p>`
  });

  res.json({ message: 'Kod mailinize gönderildi' });
});

router.post('/auth/reset-password', async (req, res) => {
  const { email, code, newPassword } = req.body;
  const user = await db.User.findOne({ where: { email } });
  if (!user) return res.status(404).json({ message: 'Kullanıcı bulunamadı' });

  if (user.verificationCode !== code || new Date() > user.codeExpiresAt) {
    return res.status(400).json({ message: 'Geçersiz veya süresi dolmuş kod' });
  }

  user.passwordHash = await bcrypt.hash(newPassword, 10);
  user.verificationCode = null;
  user.codeExpiresAt = null;
  await user.save();

  res.json({ message: 'Şifre başarıyla değiştirildi' });
});





module.exports = router;
