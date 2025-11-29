// routes/authRoutes.js
const express = require('express');
const router = express.Router();
const jwt = require('jsonwebtoken');
const bcrypt = require('bcrypt');
const db = require('../models');

// 🔴 YENİ: Güçlü şifre regex'i
// En az 1 büyük harf, en az 1 özel karakter, minimum 8 karakter
const strongPasswordRegex = /^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$/;

function signToken(user) {
  return jwt.sign(
    { sub: user.id, email: user.email },
    process.env.JWT_SECRET,
    { expiresIn: '7d' }
  );
}

// POST /api/auth/register
router.post('/auth/register', async (req, res) => {
  try {
    const { name, email, password } = req.body || {};
    if (!name || !email || !password) {
      return res.status(400).json({ message: 'name, email, password zorunlu' });
    }

    // 🔴 YENİ: Şifre kurallarını kontrol et
    if (!strongPasswordRegex.test(password)) {
      return res.status(400).json({
        message:
          'Şifre en az 8 karakter olmalı, en az bir büyük harf ve en az bir özel karakter içermelidir.'
      });
    }

    // 🔴 YENİ: Kullanıcı adı (name) daha önce alınmış mı?
    const nameExists = await db.User.findOne({ where: { name } });
    if (nameExists) {
      return res.status(409).json({ message: 'Bu kullanıcı adı zaten alınmış' });
    }

    // ZATEN VARDI: Email kontrolü
    const exists = await db.User.findOne({ where: { email } });
    if (exists) {
      return res.status(409).json({ message: 'Bu e-posta zaten kayıtlı' });
    }

    // Şifreyi hashle
    const passwordHash = await bcrypt.hash(password, 10);

    // Kullanıcıyı oluştur
    const user = await db.User.create({ name, email, passwordHash });

    const token = signToken(user);
    console.log('📝 Kayıt:', { id: user.id, email: user.email });
    return res.json({
      token,
      user: { id: String(user.id), name: user.name, email: user.email }
    });
  } catch (e) {
    console.error('register hata:', e);
    return res.status(500).json({ message: 'Sunucu hatası' });
  }
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

    const ok = await user.verifyPassword(password);
    if (!ok) return res.status(401).json({ message: 'Geçersiz kimlik bilgileri' });

    const token = signToken(user);
    console.log('🔑 Login:', { id: user.id, email: user.email });
    return res.json({
      token,
      user: { id: String(user.id), name: user.name, email: user.email }
    });
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

    const payload = jwt.verify(token, process.env.JWT_SECRET);
    const user = await db.User.findByPk(payload.sub);
    if (!user) return res.status(401).json({ message: 'Yetkisiz' });

    return res.json({ id: String(user.id), name: user.name, email: user.email });
  } catch (e) {
    return res.status(401).json({ message: 'Yetkisiz' });
  }
});

module.exports = router;
