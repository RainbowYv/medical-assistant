

# 硅谷小智 - 医疗智能体助手 (Medical AI Assistant)

[![Java](https://img.shields.io/badge/Java-18+-orange.svg)](https://www.oracle.com/java/technologies/javase-jdk18-downloads.html)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![LangChain4j](https://img.shields.io/badge/LangChain4j-1.0.0--beta3-blue.svg)](https://github.com/langchain4j/langchain4j)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](LICENSE)

**硅谷小智** 是一款基于 **LangChain4j** 框架开发的垂直领域医疗智能体。它集成了检索增强生成 (RAG)、自动化知识库构建、多模态文档解析以及外部工具调用（Function Calling）等核心 AI 技术，旨在为医院提供智能分诊、挂号预约、医疗咨询等一站式服务。

---

## 🌟 项目亮点

-   **全自动化 RAG 流程**：支持 PDF、Markdown、TXT 格式文档的自动解析、递归切割及向量化存入 Qdrant。
-   **本地化隐私部署**：使用 **Ollama** 在本地运行嵌入模型 (`nomic-embed-text`)，确保医疗数据不出库。
-   **多维记忆持久化**：结合 **MongoDB** 存储患者聊天历史，实现多轮对话的上下文感知。
-   **智能工具调用**：智能体可自动调用内部接口（MySQL 驱动）执行预约挂号、取消挂号等业务操作。
-   **环境一键启动**：提供完整的 `docker-compose` 方案，实现数据库、向量库、消息队列的秒级初始化。

---

## 🛠️ 技术架构

-   **核心框架**：Spring Boot 3.x, LangChain4j
-   **大模型接口**：OpenAI / DeepSeek / Ollama
-   **向量数据库**：Qdrant (gRPC 模式)
-   **持久化层**：MySQL 8.0 (业务数据), MongoDB 7.0 (聊天记忆)
-   **文档解析**：Apache PDFBox
-   **接口文档**：Knife4j (OpenAPI 3)

---

## 📂 项目结构

```text
medical-assistent-ai
├── src/main/java
│   ├── com.rainbow.assistant   # AI Service 定义
│   ├── com.rainbow.config      # LangChain4j, Qdrant, MongoDB 配置及自动初始化
│   ├── com.rainbow.controller  # 对话与知识库管理接口
│   ├── com.rainbow.service     # 预约挂号业务逻辑
│   └── com.rainbow.tools       # AI 工具类 (Function Calling)
├── knowledge/                  # 医疗知识库目录 (支持 .pdf, .md, .txt)
├── sql/                        # MySQL 自动初始化脚本
├── docker-compose.yml          # 环境配置
└── src/main/resources
    ├── application.yml         # 项目核心配置文件
    └── system-prompt-template.txt # AI 角色设置及约束策略
```

---

## 🚀 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/RainbowYv/medical-assistant.git
cd medical-assistent
```

### 2. 环境准备 (Docker)
确保本地已安装 Docker，在项目根目录运行：
```bash
docker-compose up -d
```
**这会自动启动 MySQL(3307), MongoDB(27017), Qdrant(6333/6334)。**

### 3. 本地 LLM 准备 (Ollama)
安装 [Ollama](https://ollama.com/) 并下载本项目所需的嵌入模型：
```bash
ollama pull nomic-embed-text
```

### 4. 配置修改
在 `src/main/resources/application.yml` 中修改你的 OpenAI/DeepSeek API Key：
```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: 
```

### 5. 运行项目
将你的医疗文档（如科室介绍、诊疗指南）放入 `knowledge/` 目录，然后运行 `XiaozhiApp.java`。

**项目启动时会自动完成以下操作：**
1. 检查并在 Qdrant 中创建 `medical_knowledge` 集合。
2. 扫描 `knowledge/` 目录，自动解析并向量化所有文档。
3. 初始化 MySQL 业务表结构。

---

## 📖 接口使用

项目集成 **Knife4j**，启动后访问：
`http://localhost:8080/doc.html`

### 测试对话示例
- **基础咨询**：发送 `{"memoryId": 1, "message": "你好，你是谁？"}`
- **医疗 RAG**：发送 `{"memoryId": 1, "message": "神经内科在几楼？偏头痛怎么治？"}`
- **挂号操作**：发送 `{"memoryId": 1, "message": "帮我预约明天下午神经内科的张医生，我叫张三，身份证号 110101..."}`

---

## ⚠️ 安全免责声明
本项目生成的回复内容仅供参考，不构成专业的医疗建议、诊断或治疗。在进行任何医疗决策前，请务必咨询专业医生。

---

## 🤝 贡献与反馈
欢迎提交 Issue 或 Pull Request 来完善这个项目。
如果你觉得这个项目对你有帮助，请给一个 ⭐️ **Star**！

---
