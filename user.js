// models/user.js
const { DataTypes } = require('sequelize');
const bcrypt = require('bcrypt');

module.exports = (sequelize) => {
  const User = sequelize.define(
    'User',
    {
      id: {
        type: DataTypes.BIGINT,
        primaryKey: true,
        autoIncrement: true,
        field: 'id',
      },
      name: {
        type: DataTypes.TEXT,
        allowNull: false,
        field: 'name',
      },
      email: {
        type: DataTypes.TEXT,
        allowNull: false,
        unique: true,
        validate: { isEmail: true },
        field: 'email',
      },
      passwordHash: {
        type: DataTypes.TEXT,
        allowNull: false,
        field: 'passwordhash',
      },
      role: {
        type: DataTypes.TEXT,
        allowNull: false,
        defaultValue: 'user',
        validate: { isIn: [['user', 'admin']] },
        field: 'role',
      },
      isVerified: {                 //mail doğrulanması için
        type: DataTypes.BOOLEAN,
        allowNull: false,
        defaultValue: false,
        field: 'isverified',
      },
      verificationCode: {       // 4 haneli doğrulama kodu için
        type: DataTypes.STRING,
        allowNull: true,
        field: 'verificationcode',
      },
      codeExpiresAt: {          // Kodun geçerlilik süresi
        type: DataTypes.DATE,
        allowNull: true,
        field: 'codeexpiresat',
      },
    },
    {
      tableName: 'users',
      timestamps: true,
      createdAt: 'createdat',
      updatedAt: 'updatedat',
    }
  );

  // Şifre doğrulama metodu
  User.prototype.verifyPassword = function (password) {
    return bcrypt.compare(password, this.passwordHash);
  };

  return User;
};
