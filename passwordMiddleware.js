module.exports = (req, res, next) => {
  const { password } = req.body;

  if (!password) {
    return res.status(400).json({
      success: false,
      message: "Şifre alanı zorunludur.",
    });
  }

  const regex = /^(?=.*[A-Z])(?=.*[^A-Za-z0-9]).{8,}$/;

  if (!regex.test(password)) {
    return res.status(400).json({
      success: false,
      message:
        "Şifre en az 8 karakter olmalı, 1 büyük harf ve 1 özel karakter içermelidir.",
    });
  }

  next();
};
