import { api } from './api.js';

/**
 * AgroVerse Farmer Assistant (Kisan Sahayak)
 * Redesigned for visual excellence and farmer-friendly interactions.
 */

function initChatbot() {
    if (document.getElementById('agro-chatbot')) return;

    const chatbotHTML = `
        <div id="agro-chatbot" class="chatbot-container">
            <div class="chatbot-header">
                <div class="header-main">
                    <img src="assets/ai-assistant.png" alt="AgroBot" class="bot-avatar">
                    <div class="header-text">
                        <h3 data-i18n="chatbot.title">Kisan Sahayak</h3>
                        <span class="status-indicator" data-i18n="chatbot.subtitle">Online for your help</span>
                    </div>
                </div>
                <button id="close-chatbot" title="Close"><i class="fas fa-chevron-down"></i></button>
            </div>
            
            <div id="chatbot-messages" class="chatbot-messages">
                <div class="bot-message-wrapper">
                    <div class="message bot">
                        <i class="fas fa-seedling bot-icon"></i>
                        <span data-i18n="chatbot.welcome">नमस्ते! मैं आपका कृषि सहायक हूँ। मैं आपकी फसल की देखभाल, बाजार भाव और मौसम की जानकारी में मदद कर सकता हूँ।</span>
                    </div>
                    <div class="msg-actions">
                        <button class="speak-msg-btn" title="Listen"><i class="fas fa-volume-up"></i></button>
                    </div>
                </div>
                <div class="quick-tips">
                    <button class="tip-btn" data-query="Farming tips for this month" data-i18n="chatbot.tipMonthly">📅 Monthly Tips</button>
                    <button class="tip-btn" data-query="Latest market prices" data-i18n="chatbot.tipMarket">💰 Market Rates</button>
                    <button class="tip-btn" data-query="Weather forecast for NASHIK" data-i18n="chatbot.tipWeather">⛅ Weather</button>
                </div>
            </div>

            <div class="chatbot-input-area">
                <div class="input-actions-left">
                    <button id="media-btn" title="Upload Media"><i class="fas fa-paperclip"></i></button>
                    <button id="camera-btn" title="Open Camera"><i class="fas fa-camera"></i></button>
                </div>
                <input type="text" id="chat-input" data-i18n-placeholder="chatbot.placeholder" placeholder="Type your question here...">
                <button id="send-chat"><i class="fas fa-paper-plane"></i></button>
                <input type="file" id="media-input" style="display: none;" accept="image/*">
            </div>
        </div>

        <button id="chatbot-toggle" class="chatbot-toggle">
            <div class="toggle-icon-wrapper">
                <img src="assets/ai-assistant.png" alt="AI">
            </div>
        </button>
    `;

    document.body.insertAdjacentHTML('beforeend', chatbotHTML);

    // Styles for Farmer-Friendly Look
    const styles = `
        @keyframes slideUp { from { transform: translateY(50px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
        @keyframes pulse { 0% { box-shadow: 0 0 0 0 rgba(46, 160, 67, 0.4); } 70% { box-shadow: 0 0 0 15px rgba(46, 160, 67, 0); } 100% { box-shadow: 0 0 0 0 rgba(46, 160, 67, 0); } }

        .chatbot-toggle {
            position: fixed;
            bottom: 30px;
            right: 30px;
            width: 70px;
            height: 70px;
            border-radius: 50%;
            background: var(--bg-card);
            border: 3px solid #2ea043;
            box-shadow: 0 8px 25px rgba(0,0,0,0.2);
            cursor: pointer;
            z-index: 9999;
            padding: 0;
            overflow: hidden;
            animation: pulse 2s infinite;
            transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
        }

        .chatbot-toggle:hover {
            transform: scale(1.1) rotate(5deg);
        }

        .toggle-icon-wrapper img {
            width: 100%;
            height: 100%;
            object-fit: cover;
        }

        .chatbot-container {
            position: fixed;
            bottom: 110px;
            right: 30px;
            width: 380px;
            height: 520px;
            background: var(--bg-card);
            border-radius: 24px;
            box-shadow: var(--shadow-card);
            display: none;
            flex-direction: column;
            z-index: 9998;
            overflow: hidden;
            border: 1px solid var(--border-color);
            animation: slideUp 0.4s ease-out;
        }

        .chatbot-header {
            padding: 20px;
            background: linear-gradient(135deg, var(--color-green-dark) 0%, var(--color-green) 100%);
            color: white;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }

        .header-main { display: flex; align-items: center; gap: 12px; }
        .bot-avatar { width: 45px; height: 45px; border-radius: 50%; border: 2px solid var(--color-surface); background: var(--color-surface); }
        .header-text h3 { margin: 0; font-size: 18px; letter-spacing: 0.5px; color: white; }
        .status-indicator { font-size: 11px; opacity: 0.9; display: flex; align-items: center; gap: 5px; color: #fff; }
        .status-indicator::before { content: ''; width: 8px; height: 8px; background: #4dfd4d; border-radius: 50%; display: inline-block; }

        #close-chatbot { background: transparent; border: none; color: white; cursor: pointer; font-size: 18px; padding: 5px; }

        .chatbot-messages {
            flex: 1;
            padding: 20px;
            overflow-y: auto;
            background: var(--bg-main);
            display: flex;
            flex-direction: column;
            gap: 15px;
        }

        .message {
            padding: 12px 18px;
            border-radius: 20px;
            font-size: 15px;
            line-height: 1.5;
            max-width: 85%;
            position: relative;
        }

        .message.bot { 
            background: var(--color-surface-soft); 
            color: var(--color-text); 
            align-self: flex-start; 
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            border: 1px solid var(--color-border);
            border-bottom-left-radius: 4px;
        }
        .bot-icon { color: #2ea043; margin-right: 8px; font-size: 12px; }

        .message.user { 
            background: var(--color-green); 
            color: white; 
            align-self: flex-end; 
            border-bottom-right-radius: 4px;
            box-shadow: 0 4px 12px rgba(34, 197, 94, 0.2);
        }

        .quick-tips { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
        .tip-btn { 
            background: var(--bg-card); 
            border: 1px solid var(--border-color); 
            padding: 6px 12px; 
            border-radius: 15px; 
            font-size: 12px; 
            color: var(--text-muted); 
            cursor: pointer; 
            transition: all 0.2s; 
        }
        .tip-btn:hover { background: var(--bg-main); border-color: #2ea043; color: #2ea043; }

        .chatbot-input-area {
            padding: 18px 20px;
            background: var(--bg-card);
            border-top: 1px solid var(--border-color);
            display: flex;
            gap: 12px;
            align-items: center;
        }

        #chat-input {
            flex: 1;
            border: 1px solid var(--border-color);
            background: var(--input-bg);
            color: var(--text-main);
            padding: 12px 20px;
            border-radius: 30px;
            font-size: 14px;
            outline: none;
            transition: border-color 0.2s;
        }
        #chat-input:focus { border-color: #2ea043; }

        #send-chat {
            background: var(--color-green);
            color: white;
            border: none;
            width: 42px;
            height: 42px;
            border-radius: 50%;
            cursor: pointer;
            display: flex;
            align-items: center;
            justify-content: center;
            transition: transform 0.2s;
        }
        #send-chat:hover { transform: scale(1.1); opacity: 0.9; }

        .header-actions { display: flex; gap: 10px; align-items: center; }
        #toggle-voice { background: rgba(255,255,255,0.2); border: none; color: white; cursor: pointer; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
        #toggle-voice.active { background: white; color: var(--color-green); }
        #toggle-voice:hover { background: rgba(255,255,255,0.3); }

        .input-actions-left { display: flex; gap: 8px; margin-right: 5px; }
        .input-actions-left button { background: transparent; border: none; color: var(--text-muted); cursor: pointer; font-size: 18px; padding: 5px; transition: color 0.2s; }
        .input-actions-left button:hover { color: var(--color-green); }

        .camera-overlay { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: black; z-index: 10000; display: none; flex-direction: column; }
        .camera-overlay video { flex: 1; object-fit: cover; }
        .camera-controls { padding: 20px; display: flex; justify-content: space-around; background: rgba(0,0,0,0.8); }
        .camera-controls button { background: white; border: none; padding: 10px 20px; border-radius: 30px; cursor: pointer; font-weight: bold; }

        .bot-message-wrapper { display: flex; flex-direction: column; align-items: flex-start; gap: 5px; max-width: 85%; }
        .message.bot { max-width: 100%; width: fit-content; }
        .msg-actions { display: flex; gap: 10px; margin-left: 5px; opacity: 0.6; transition: opacity 0.2s; }
        .msg-actions:hover { opacity: 1; }
        .speak-msg-btn { background: transparent; border: none; color: var(--text-muted); cursor: pointer; font-size: 14px; padding: 2px; }
        .speak-msg-btn:hover { color: var(--color-green); }
        .speak-msg-btn.speaking { color: var(--color-green); animation: pulse 1s infinite; }
    `;

    // Add camera overlay to container
    const cameraOverlayHTML = `
        <div id="camera-overlay" class="camera-overlay">
            <video id="camera-video" autoplay playsinline></video>
            <div class="camera-controls">
                <button id="capture-photo">Take Photo</button>
                <button id="cancel-camera">Cancel</button>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', cameraOverlayHTML);

    const styleSheet = document.createElement("style");
    styleSheet.innerText = styles;
    document.head.appendChild(styleSheet);

    // Functionality
    const toggleBtn = document.getElementById('chatbot-toggle');
    const closeBtn = document.getElementById('close-chatbot');
    const chatbot = document.getElementById('agro-chatbot');
    const chatInput = document.getElementById('chat-input');
    const sendBtn = document.getElementById('send-chat');
    const messagesContainer = document.getElementById('chatbot-messages');
    const cameraBtn = document.getElementById('camera-btn');
    const mediaBtn = document.getElementById('media-btn');
    const mediaInput = document.getElementById('media-input');
    const cameraOverlay = document.getElementById('camera-overlay');
    const cameraVideo = document.getElementById('camera-video');
    const captureBtn = document.getElementById('capture-photo');
    const cancelCameraBtn = document.getElementById('cancel-camera');

    let currentUtterance = null;

    function speak(text, btn = null) {
        window.speechSynthesis.cancel();
        if (btn && btn.classList.contains('speaking')) {
            btn.classList.remove('speaking');
            return;
        }

        // Reset all buttons
        document.querySelectorAll('.speak-msg-btn').forEach(b => b.classList.remove('speaking'));
        if (btn) btn.classList.add('speaking');

        currentUtterance = new SpeechSynthesisUtterance(text);
        
        if (/[\u0900-\u097F]/.test(text)) {
            currentUtterance.lang = 'hi-IN';
        } else {
            currentUtterance.lang = 'en-IN';
        }

        currentUtterance.onend = () => {
            if (btn) btn.classList.remove('speaking');
        };

        window.speechSynthesis.speak(currentUtterance);
    }

    mediaBtn.onclick = () => mediaInput.click();
    mediaInput.onchange = (e) => {
        const file = e.target.files[0];
        if (file) {
            appendMessage('user', `Uploaded file: ${file.name}`);
            // Logic to handle file upload to backend can be added here
            sendMessage(`Analyzing uploaded image: ${file.name}`);
        }
    };

    cameraBtn.onclick = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
            cameraVideo.srcObject = stream;
            cameraOverlay.style.display = 'flex';
        } catch (err) {
            console.error("Camera access error:", err);
            alert("Unable to access camera.");
        }
    };

    cancelCameraBtn.onclick = () => {
        const stream = cameraVideo.srcObject;
        if (stream) stream.getTracks().forEach(track => track.stop());
        cameraOverlay.style.display = 'none';
    };

    captureBtn.onclick = () => {
        const canvas = document.createElement('canvas');
        canvas.width = cameraVideo.videoWidth;
        canvas.height = cameraVideo.videoHeight;
        canvas.getContext('2d').drawImage(cameraVideo, 0, 0);
        const dataUrl = canvas.toDataURL('image/png');
        
        cancelCameraBtn.click();
        appendMessage('user', `<img src="${dataUrl}" style="max-width: 100%; border-radius: 10px;">`);
        sendMessage("Analyzing captured photo...");
    };

    toggleBtn.onclick = () => {
        const isOpening = chatbot.style.display !== 'flex';
        chatbot.style.display = isOpening ? 'flex' : 'none';
        if (isOpening) chatInput.focus();
    };

    closeBtn.onclick = () => chatbot.style.display = 'none';

    async function sendMessage(textOverride = null) {
        const text = textOverride || chatInput.value.trim();
        if (!text) return;

        appendMessage('user', text);
        chatInput.value = '';
        sendBtn.disabled = true;

        const typingId = 'typing-' + Date.now();
        const typingEl = document.createElement('div');
        typingEl.className = 'message bot';
        typingEl.id = typingId;
        const typingText = typeof i18next !== 'undefined' ? i18next.t('chatbot.typing') : 'Assistant is thinking...';
        typingEl.innerHTML = `<i class="fas fa-leaf fa-spin bot-icon"></i> ${typingText}`;
        messagesContainer.appendChild(typingEl);
        messagesContainer.scrollTop = messagesContainer.scrollHeight;

        try {
            // Using the centralized API module
            const data = await api.chatbot(text);
            typingEl.remove();
            const reply = data.reply || "I'm sorry, I couldn't understand that.";
            appendMessage('bot', reply);
        } catch (error) {
            typingEl.remove();
            console.error("Chatbot connection error:", error);
            const errorMsg = "Connection error. Backend API is not reachable.";
            appendMessage('bot', errorMsg);
        } finally {
            sendBtn.disabled = false;
        }
    }

    function appendMessage(type, text) {
        if (type === 'bot') {
            const wrapper = document.createElement('div');
            wrapper.className = 'bot-message-wrapper';
            
            const msg = document.createElement('div');
            msg.className = 'message bot';
            msg.innerHTML = `<i class="fas fa-leaf bot-icon"></i> ${text}`;
            
            const actions = document.createElement('div');
            actions.className = 'msg-actions';
            actions.innerHTML = `
                <button class="speak-msg-btn" title="Listen"><i class="fas fa-volume-up"></i></button>
            `;
            
            actions.querySelector('.speak-msg-btn').onclick = function() {
                speak(text, this);
            };
            
            wrapper.appendChild(msg);
            wrapper.appendChild(actions);
            messagesContainer.appendChild(wrapper);
        } else {
            const msg = document.createElement('div');
            msg.className = `message ${type}`;
            msg.textContent = text;
            messagesContainer.appendChild(msg);
        }
        messagesContainer.scrollTop = messagesContainer.scrollHeight;
    }

    sendBtn.onclick = () => sendMessage();
    chatInput.onkeypress = (e) => { if (e.key === 'Enter') sendMessage(); };

    // Quick tips functionality
    document.querySelectorAll('.tip-btn').forEach(btn => {
        btn.onclick = () => sendMessage(btn.getAttribute('data-query'));
    });

    // Initial welcome message speaker icon
    const initialSpeakBtn = messagesContainer.querySelector('.speak-msg-btn');
    if (initialSpeakBtn) {
        const welcomeText = messagesContainer.querySelector('.message.bot span').textContent;
        initialSpeakBtn.onclick = () => speak(welcomeText, initialSpeakBtn);
    }
}

// Initialize
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initChatbot);
} else {
    initChatbot();
}
