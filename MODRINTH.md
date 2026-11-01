# 🔮 MineColonies Iron's Integration

[![Minecraft 1.21.1](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)](https://modrinth.com/)
[![NeoForge](https://img.shields.io/badge/Modloader-NeoForge-orange.svg)](https://neoforged.net/)
[![License](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

> **Empower your colony defenses with Arcane Magic and Black Powder Firepower! Adds dedicated Mage and Gunner guard professions with authentic spellcasting, native mana, firearms, and university research.**

---

## 📖 Overview

**MineColonies Iron's Integration** bridges the gap between **MineColonies** and the legendary **Iron's Spells 'n Spellbooks** & **Iron's Arms 'n Artifice** mods on **Minecraft 1.21.1 (NeoForge)**.

Train specialized colony defenders who don't just mimic players—they use the exact same combat systems! Mages cast spells directly inscribed inside their held spellbooks powered by an integrated mana system, while Gunners unleash volleys of black powder rifle and pistol fire with authentic projectile physics and smoke particles.

---

## ✨ Features

### 🔮 1. The Mage Profession (Iron's Spells)
* **Spellbook Inscription Casting:** Mages cast the exact spells inscribed in their assigned Spellbook! If a spellbook is empty, they fall back to a baseline *Magic Missile I*.
* **Native Mana & Attribute System:** Citizens possess their own max mana and mana regeneration pools. Wands, staves, accessories, and potion effects dynamically enhance citizen mana pools in real-time.
* **Persistent Spellbook & Staff Rendering:** Mages hold and visibly display their spellbooks in their offhand and staves/wands in their main hand at all times.
* **Mage Robes & Armor Unlock (Research Level 2):** Unlock the ability for Mages to don Iron's Spells wizard robes, gaining school-specific spell power bonuses and cooldown reductions.
* **Authentic Projectiles & Effects:** True Iron's Spells projectile entities, spell animations, audio effects, and casting stances.

---

### 🔫 2. The Gunner Profession (Iron's Artifice)
* **Firearms Arsenal:** Gunners wield firearms from Iron's Artifice, including Pistols, Muskets, Blunderbusses, and Rifles.
* **Strict Ammunition System:** Gunners strictly utilize authentic Iron's Artifice ammunition (Iron/Lead Bullets).
* **Player-grade Ballistics:** Guns fire true ballistics in the exact direction the guard is facing, featuring muzzle flashes, gunpowder smoke, authentic gunshot sounds, and target knockback.
* **Dynamic Weapon Sync:** When switching weapons in the guard tower inventory, the gun model immediately updates in the citizen's hand.

---

### 🎓 3. University Research Tree
Both professions seamlessly integrate into the MineColonies University combat tree:
* 📜 **Arcane Mastery (Combat Branch - Level 1):** Unlocks the Mage profession in Guard Towers and Barracks Towers.
* 🥋 **Wizard Vestments (Combat Branch - Level 2):** Unlocks the Mage Robes tier for advanced spell power and armor defenses.
* 🎯 **Firearms Training (Combat Branch - Level 1):** Unlocks the Gunner profession in Guard Towers and Barracks Towers.

---

### 🤝 4. Seamless MCA Reborn Compatibility
* Fully compatible with **[MCA x MineColonies Integration](https://modrinth.com/)**!
* 3D humanoid player models (Steve & Alex) naturally wear Mage Robes, wield firearms, hold spellbooks, and assume combat stances during battles.

---

## 📦 Requirements

| Mod | Version | Required? |
| :--- | :--- | :--- |
| **Minecraft** | `1.21.1` | **Required** |
| **NeoForge** | `21.1.x`+ | **Required** |
| **MineColonies** | `1.21.1` (with Structurize & BlockUI) | **Required** |
| **Iron's Spells 'n Spellbooks** | `1.21.1` | **Required** (for Mage) |
| **Iron's Arms 'n Artifice** | `1.21.1` | **Required** (for Gunner) |

---

## ❓ Frequently Asked Questions (FAQ)

#### Q: How do Mages choose what spells to cast?
**A:** Mages inspect the spellbook in their hand! They will randomly cycle through and cast any spells currently inscribed into the book. If no spells are inscribed, they cast basic *Magic Missile I*.

#### Q: Do items that boost Mana work on Mages?
**A:** Yes! The mod injects Iron's Spells attribute modifiers directly onto citizens. Rings, robes, spellbooks, and staves that provide `Max Mana` or `Mana Regen` will boost the Mage's casting capabilities.

#### Q: What ammunition do Gunners need?
**A:** Gunners require official Iron's Artifice bullets in their inventory or guard tower chest. They will not fire without appropriate ammunition.

#### Q: Can I run this with only Iron's Spells or only Iron's Artifice?
**A:** Yes! The mod handles both gracefully. If only Iron's Spells is installed, Mages will be active. If only Iron's Artifice is installed, Gunners will be active.

---

## 🇹🇭 คำอธิบายภาษาไทย (Thai Description)
ม็อดเสริมความสามารถให้ทหารยามของ **MineColonies** ร่วมกับ **Iron's Spells 'n Spellbooks** และ **Iron's Arms 'n Artifice**:
* **อาชีพ Mage (นักเวท):** ใช้เวทมนตร์จริงตามที่บันทึกไว้ใน Spellbook ที่ถือ มีระบบมานา (Mana Pool & Regen) ของตัวเอง สวมใส่ชุดคลุมเวทมนตร์ (Mage Robes) เพิ่มพลังโจมตีเวทมนตร์ตามสาย และถือคทากับสมุดเวทแสดงผลบนตัวตลอดเวลา
* **อาชีพ Gunner (มือปืน):** ใช้อาวุธปืนคาบศิลา ปืนพก ปืนไรเฟิล และกระสุนจากม็อด Iron's Artifice ยิงกระสุนจริงตามทิศทางที่หัน พร้อมเอฟเฟกต์ควันปืนและประกายไฟปากกระบอก
* **สายวิจัยในมหาวิทยาลัย (University):** ปลดล็อกอาชีพ Mage, Gunner และชุด Mage Robes ผ่านสายวิจัยการต่อสู้ (Combat Research)
* **รองรับ MCA Reborn:** ใช้งานร่วมกับม็อดโมเดลคน 3D ได้อย่างสมบูรณ์แบบ
