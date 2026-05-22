const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const jwt = require('jsonwebtoken');
const db = require('../database');

const router = express.Router();
const JWT_SECRET = process.env.JWT_SECRET || 'meetngo-secret-key-change-in-production';

const uploadsDir = path.join(__dirname, '../uploads');
if (!fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir, { recursive: true });

const storage = multer.diskStorage({
  destination: uploadsDir,
  filename: (req, file, cb) => {
    const unique = `${Date.now()}-${Math.round(Math.random() * 1e9)}${path.extname(file.originalname)}`;
    cb(null, unique);
  },
});

const upload = multer({
  storage,
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    cb(null, ['image/jpeg', 'image/png', 'image/webp'].includes(file.mimetype));
  },
});

function authMiddleware(req, res, next) {
  const token = req.headers.authorization?.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Nicht authentifiziert' });
  try {
    req.user = jwt.verify(token, JWT_SECRET);
    next();
  } catch {
    res.status(401).json({ error: 'Ungültiger Token' });
  }
}

// GET /api/events?search=&category=&sort=date|name|attendees|price&order=asc|desc&priceFilter=free|paid
router.get('/', (req, res) => {
  const { search = '', category = '', sort = 'date', order = 'asc', priceFilter = '' } = req.query;

  const validSorts = { date: 'date', name: 'name', attendees: 'attendees', price: 'price' };
  const sortCol = validSorts[sort] || 'date';
  const sortOrder = order === 'desc' ? 'DESC' : 'ASC';

  let query = 'SELECT * FROM events WHERE 1=1';
  const params = [];

  if (search) {
    query += ' AND (name LIKE ? OR location LIKE ? OR description LIKE ? OR organizer LIKE ?)';
    params.push(`%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`);
  }

  if (category && category !== 'Alle') {
    query += ' AND category = ?';
    params.push(category);
  }

  if (priceFilter === 'free') {
    query += " AND price = 'Kostenlos'";
  } else if (priceFilter === 'paid') {
    query += " AND price != 'Kostenlos'";
  }

  query += ` ORDER BY ${sortCol} ${sortOrder}`;

  const events = db.prepare(query).all(...params);
  res.json(events);
});

router.get('/:id', (req, res) => {
  const event = db.prepare('SELECT * FROM events WHERE id = ?').get(req.params.id);
  if (!event) return res.status(404).json({ error: 'Event nicht gefunden' });
  res.json(event);
});

router.post('/', authMiddleware, upload.single('image'), (req, res) => {
  const { name, description, date, location, lat, lng, price, capacity, category, featured } = req.body;

  if (!name || !date || !location) {
    if (req.file) fs.unlinkSync(req.file.path);
    return res.status(400).json({ error: 'Name, Datum und Ort sind erforderlich' });
  }

  const imagePath = req.file ? `/uploads/${req.file.filename}` : null;

  const stmt = db.prepare(`
    INSERT INTO events (name, description, date, location, lat, lng, organizer, organizer_id, price, capacity, category, image_path, featured)
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
  `);

  const result = stmt.run(
    name,
    description || null,
    date,
    location,
    parseFloat(lat) || 49.0069,
    parseFloat(lng) || 8.4037,
    req.user.username,
    req.user.userId,
    price || 'Kostenlos',
    parseInt(capacity) || null,
    category || 'Sonstiges',
    imagePath,
    featured === 'true' ? 1 : 0
  );

  const event = db.prepare('SELECT * FROM events WHERE id = ?').get(result.lastInsertRowid);
  res.status(201).json(event);
});

router.delete('/:id', authMiddleware, (req, res) => {
  const event = db.prepare('SELECT * FROM events WHERE id = ?').get(req.params.id);
  if (!event) return res.status(404).json({ error: 'Event nicht gefunden' });
  if (event.organizer_id !== req.user.userId) {
    return res.status(403).json({ error: 'Keine Berechtigung' });
  }

  if (event.image_path) {
    const imgFile = path.join(__dirname, '..', event.image_path);
    if (fs.existsSync(imgFile)) fs.unlinkSync(imgFile);
  }

  db.prepare('DELETE FROM events WHERE id = ?').run(req.params.id);
  res.json({ success: true });
});

module.exports = router;
