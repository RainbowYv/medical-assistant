<template>
  <div class="app-layout">
    <div class="sidebar">
      <div class="logo-section">
        <img src="@/assets/logo.png" alt="硅谷小智" class="sidebar-logo" />
        <span class="logo-text">硅谷小智</span>
        <span class="logo-subtext">医疗服务版</span>
      </div>
      <el-button class="new-chat-button" @click="newChat" type="primary" plain>
        <i class="fa-solid fa-plus"></i>
        &nbsp;开始新会话
      </el-button>
    </div>

    <div class="main-content">
      <div class="chat-header">
        <div class="status-indicator">
          <i class="fa-solid fa-robot text-primary"></i>
          <span class="header-title">智能医疗助手已上线</span>
        </div>
      </div>

      <div class="chat-container">
        <div class="message-list markdown-body" ref="messaggListRef">
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="[
              'message-wrapper',
              message.isUser ? 'user-wrapper' : 'bot-wrapper'
            ]"
          >
            <div class="avatar-container">
              <i
                :class="
                  message.isUser
                    ? 'fa-solid fa-circle-user user-avatar-icon'
                    : 'fa-solid fa-notes-medical bot-avatar-icon'
                "
              ></i>
            </div>

            <div class="message-bubble">
              <div
                class="message-content-text"
                v-html="renderMarkdown(message.content)"
              ></div>

              <span
                class="loading-dots-v2"
                v-if="message.isThinking || message.isTyping"
              >
                <span>.</span><span>.</span><span>.</span>
              </span>
            </div>
          </div>
        </div>

        <div class="input-container-v2">
          <el-input
            v-model="inputMessage"
            placeholder="请在此输入您的病情描述或医疗疑问..."
            @keyup.enter="sendMessage"
            :disabled="isSending"
            clearable
          ></el-input>
          <el-button @click="sendMessage" :loading="isSending" type="primary">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, watch, nextTick } from 'vue'
import axios from 'axios'
import { v4 as uuidv4 } from 'uuid'
// [修改核心] 引入依赖包
import { marked } from 'marked'
import DOMPurify from 'dompurify'

// 配置 marked，例如支持换行
marked.setOptions({
  breaks: true, // 支持在 Markdown 中直接使用换行符
});

const messaggListRef = ref()
const isSending = ref(false)
const uuid = ref()
const inputMessage = ref('')
const messages = ref([])

onMounted(() => {
  initUUID()
  // 移除以前的监听，直接在请求结束时滚动
  hello()
})

const scrollToBottom = () => {
  nextTick(() => {
    if (messaggListRef.value) {
      messaggListRef.value.scrollTop = messaggListRef.value.scrollHeight
    }
  })
}

// [修改核心] 核心：Markdown 渲染与安全净化函数
const renderMarkdown = (content) => {
  if (!content) return '';
  // 1. 将 Markdown 源码解析为 HTML
  const rawHtml = marked(content);
  // 2. 净化 HTML，移除潜在恶意脚本 (防止 XSS)
  const sanitizedHtml = DOMPurify.sanitize(rawHtml);
  return sanitizedHtml;
};

const hello = () => {
  sendRequest('你好')
}

const sendMessage = () => {
  if (isSending.value) return;
  if (inputMessage.value.trim()) {
    sendRequest(inputMessage.value.trim())
    inputMessage.value = ''
  }
}

const sendRequest = (message) => {
  isSending.value = true
  const userMsg = {
    isUser: true,
    content: message,
    isTyping: false,
    isThinking: false,
  }
  //第一条默认发送的用户消息”你好“不放入会话列表
  if(messages.value.length > 0){
    messages.value.push(userMsg)
  }

  // 添加机器人加载消息
  const botMsg = {
    isUser: false,
    content: '', // 增量填充
    isTyping: true, // 显示加载动画
    isThinking: true, // 开始思考状态
  }
  messages.value.push(botMsg)
  scrollToBottom()

  axios
    .post(
      '/api/medical/assistant/chat',
      { memoryId: uuid.value, message },
      {
        responseType: 'text', // 注意：流式传输时，responseText 应对应流，这里可能是你原有配置，我统一保留
        onDownloadProgress: (e) => {
          // 在收到第一批数据时关闭“思考中”
          const currentBotMsg = messages.value[messages.value.length - 1];
          if(currentBotMsg.isThinking){
            currentBotMsg.isThinking = false;
          }

          const fullText = e.event.target.responseText // 累积的完整文本
          // 增量更新（marked会重新解析整个文本，这是正确的做法）
          currentBotMsg.content = fullText;
          scrollToBottom() // 实时滚动
        },
      }
    )
    .then(() => {
      // 流结束后隐藏加载动画
      const lastMsg = messages.value[messages.value.length - 1];
      lastMsg.isTyping = false
      lastMsg.isThinking = false
      isSending.value = false
    })
    .catch((error) => {
      console.error('流式错误:', error)
      const lastMsg = messages.value[messages.value.length - 1];
      lastMsg.content = '😔 对不起，系统暂时出现了一点小状况，未能完全理解您的意思，请您稍后再试。'
      lastMsg.isTyping = false
      lastMsg.isThinking = false
      isSending.value = false
    })
}

// 初始化 UUID
const initUUID = () => {
  let storedUUID = localStorage.getItem('user_uuid')
  if (!storedUUID) {
    storedUUID = uuidToNumber(uuidv4())
    localStorage.setItem('user_uuid', storedUUID)
  }
  uuid.value = storedUUID
}

const uuidToNumber = (uuid) => {
  let number = 0
  for (let i = 0; i < uuid.length && i < 6; i++) {
    const hexValue = uuid[i]
    number = number * 16 + (parseInt(hexValue, 16) || 0)
  }
  return number % 1000000
}

// [修改核心] 原有 naive 的 convertStreamOutput 函数已被移除，功能并入 renderMarkdown

const newChat = () => {
  // 这里添加新会话的逻辑
  console.log('开始新会话')
  localStorage.removeItem('user_uuid')
  window.location.reload()
}

</script>

<style scoped>
/* 定义全局主题颜色 */
:root {
  --primary-color: #1677ff; /* Element Plus 的默认蓝色，适合专业感 */
  --bg-main: #f0f2f5;
  --bg-sidebar: #ffffff;
  --bg-user-bubble: #e6f7ff; /* 浅蓝底色用于用户气泡 */
  --bg-bot-bubble: #ffffff; /* 纯白底色用于机器人气泡 */
  --text-main: #262626;
  --text-secondary: #595959;
}

.app-layout {
  display: flex;
  height: 100vh;
  background-color: #f0f2f5; /* 增加整体背景色，突出侧边栏和主区域 */
}

/* ================= 侧边栏优化 ================= */
.sidebar {
  width: 260px; /* 稍微加宽一点 */
  background-color: #ffffff;
  padding: 30px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border-right: 1px solid #e8e8e8;
  box-shadow: 2px 0 8px rgba(0,0,0,0.03); /* 增加轻微阴影提升立体感 */
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
}

.sidebar-logo {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 4px solid #f0f2f5;
}

.logo-text {
  font-size: 22px;
  font-weight: 700;
  color: #1677ff;
  margin-top: 15px;
}

.logo-subtext {
  font-size: 14px;
  color: #8c8c8c;
  margin-top: 4px;
}

.new-chat-button {
  width: 100%;
  height: 40px;
  font-size: 15px;
}

/* ================= 主区域优化 ================= */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px;
}

/* 顶部状态栏 */
.chat-header {
  background: #ffffff;
  padding: 15px 25px;
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 2px 4px rgba(0,0,0,0.02);
  margin-bottom: 0;
}

.status-indicator {
  display: flex;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  margin-left: 10px;
}

.text-primary {
  color: #1677ff;
  font-size: 1.2em;
}

/* 聊天主体容器 */
.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #f0f4f8; /* 调整内部聊天区域的背景色 */
  border-radius: 0 0 8px 8px;
  border: 1px solid #e0e0e0;
  overflow: hidden;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 30px;
  display: flex;
  flex-direction: column;
  scroll-behavior: smooth;
}

/* ================= 气泡布局重修 ================= */
.message-wrapper {
  display: flex;
  align-items: flex-start;
  margin-bottom: 25px;
  width: 100%;
}

/* 用户气泡样式 */
.user-wrapper {
  justify-content: flex-end;
}

/* 机器人气泡样式 */
.bot-wrapper {
  justify-content: flex-start;
}

/* 头像容器 */
.avatar-container {
  width: 36px;
  margin: 0 10px;
}

.user-wrapper .avatar-container {
  order: 2; /* 居右显示 */
}

.bot-wrapper .avatar-container {
  order: 1; /* 居左显示 */
}

/* 图标基础样式 */
.user-avatar-icon {
  color: #1677ff;
  font-size: 2.2em;
}

.bot-avatar-icon {
  color: #52c41a; /* 使用绿色代表医疗服务，更符合医学背景 */
  font-size: 2.2em;
}

/* 消息气泡主体 */
.message-bubble {
  max-width: 75%; /* 调整最大宽度 */
  padding: 14px 18px;
  border-radius: 12px;
  position: relative;
  line-height: 1.6;
}

/* 用户气泡：居右，蓝底白字 */
.user-wrapper .message-bubble {
  background-color: #1677ff;
  color: #ffffff;
  border-bottom-right-radius: 2px; /* 增加一个尖角效果 */
  order: 1;
}

/* 机器人气泡：居左，白底黑字，带轻微阴影 */
.bot-wrapper .message-bubble {
  background-color: #ffffff;
  color: #262626;
  border-bottom-left-radius: 2px; /* 增加一个尖角效果 */
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 6px rgba(0,0,0,0.03);
  order: 2;
}

/* [修改核心] ================= Markdown 内部样式定义 =================
 * marked 渲染出的各种 HTML 标签（h, p, ul, table...）需要在这里明确其样式
 */

.message-content-text {
  font-size: 15px;
}

/* Markdown 段落 */
.message-content-text p {
  margin: 0 0 10px 0;
}
.message-content-text p:last-child {
  margin-bottom: 0;
}

/* Markdown 粗体 */
.message-content-text strong {
  font-weight: 700;
}

/* Markdown 列表 (ul/ol) */
.message-content-text ul, .message-content-text ol {
  margin: 0 0 10px 20px;
  padding: 0;
}

/* Markdown 表格 (重要，针对你之前的截图) */
.message-content-text table {
  border-collapse: collapse;
  width: 100%;
  margin-bottom: 10px;
  font-size: 14px;
}
.message-content-text th, .message-content-text td {
  border: 1px solid #e0e0e0;
  padding: 6px 10px;
  text-align: left;
}
.message-content-text th {
  background-color: #f5f5f5;
  font-weight: 600;
}

/* 用户气泡内 Markdown 元素的文字颜色统一设为白色 */
.user-wrapper .message-content-text table th,
.user-wrapper .message-content-text table td {
  border-color: rgba(255,255,255,0.2);
}
.user-wrapper .message-content-text th {
  background-color: rgba(255,255,255,0.1);
  color: #ffffff;
}
.user-wrapper .message-content-text strong,
.user-wrapper .message-content-text ul,
.user-wrapper .message-content-text ol {
  color: #ffffff;
}

/* loading dots 样式优化 */
.loading-dots-v2 {
  display: inline-block;
  color: #bfbfbf;
  margin-left: 5px;
}
.loading-dots-v2 span {
  animation: loadingDots 1.4s infinite;
  font-size: 1.5em;
  font-weight: bold;
}
.loading-dots-v2 span:nth-child(2) { animation-delay: 0.2s; }
.loading-dots-v2 span:nth-child(3) { animation-delay: 0.4s; }

@keyframes loadingDots {
  0% { opacity: 0.2; }
  20% { opacity: 1; }
  100% { opacity: 0.2; }
}

/* ================= 输入区域优化 ================= */
.input-container-v2 {
  display: flex;
  background-color: #ffffff;
  padding: 15px 25px;
  border-top: 1px solid #f0f0f0;
  box-shadow: 0 -2px 6px rgba(0,0,0,0.02);
  align-items: center;
}

.input-container-v2 .el-input {
  flex: 1;
  margin-right: 15px;
}

/* 覆盖 Element Plus 输入框的默认样式 */
.input-container-v2 :deep(.el-input__wrapper) {
  height: 42px;
  border-radius: 8px;
  background-color: #f5f5f5;
  box-shadow: none;
}
.input-container-v2 :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #1677ff inset;
}

.input-container-v2 :deep(.el-button) {
  height: 42px;
  border-radius: 8px;
}

/* ================= 媒体查询 (响应式) 优化 ================= */
@media (max-width: 768px) {
  .app-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    padding: 10px 15px;
    height: auto;
    border-right: none;
    border-bottom: 1px solid #e8e8e8;
  }

  .logo-section {
    flex-direction: row;
    align-items: center;
    margin-bottom: 0;
  }

  .sidebar-logo {
    width: 40px;
    height: 40px;
    border-width: 2px;
  }

  .logo-text {
    font-size: 18px;
    margin-top: 0;
    margin-left: 10px;
  }

  .logo-subtext {
    display: none;
  }

  .new-chat-button {
    width: auto;
    font-size: 14px;
    padding: 8px 15px;
  }

  .main-content {
    padding: 10px;
  }
  
  .message-list {
    padding: 15px;
  }
  
  .message-bubble {
    max-width: 85%;
  }

  .chat-header {
    display: none; /* 移动端隐藏状态栏以节省空间 */
  }

  .chat-container {
    border-radius: 8px;
    margin-top: 10px;
  }
}
</style>