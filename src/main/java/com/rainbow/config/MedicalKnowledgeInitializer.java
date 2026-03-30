package com.rainbow.config;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.CollectionInfo;
import io.qdrant.client.grpc.Points.ScrollPoints; // 必须导入
import io.qdrant.client.grpc.Points.ScrollResponse; // 必须导入
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class MedicalKnowledgeInitializer {

    // --- 从配置文件读取参数 ---
    @Value("${medical.knowledge.path}")
    private String knowledgePath;

    @Value("${qdrant.host}")
    private String host;

    @Value("${qdrant.port}")
    private int port;

    @Value("${qdrant.collection-name}")
    private String collectionName;

    @Value("${qdrant.api-key}")
    private String apiKey;

    @Value("${qdrant.dimension}")
    private int dimension;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @PostConstruct
    public void run() {
        try (QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder(host, port, false)
                        .withApiKey(apiKey)
                        .build())) {

            // 1. 检查集合是否存在
            List<String> collections = client.listCollectionsAsync().get();
            boolean isNewCreated = false;

            if (!collections.contains(collectionName)) {
                System.out.println("【初始化】Qdrant 集合不存在，正在创建...");
                client.createCollectionAsync(collectionName,
                        VectorParams.newBuilder()
                                .setDistance(Distance.Cosine)
                                .setSize(dimension)
                                .build()
                ).get();
                isNewCreated = true;
            }

            // 2. 检查是否有数据 (使用 Scroll 替代 getPointsCount 以解决索引延迟导致的 0 计数问题)
            boolean hasData = false;
            if (!isNewCreated) {
                // 尝试查询 1 条点位数据
                ScrollResponse scrollResponse = client.scrollAsync(ScrollPoints.newBuilder()
                        .setCollectionName(collectionName)
                        .setLimit(1) // 只取 1 条记录
                        .build()).get();

                // 如果结果列表不为空，说明已经有数据了
                hasData = !scrollResponse.getResultList().isEmpty();
            }

            // 3. 只有集合是新创建的，或者原有集合内没数据时，才进行导入
            if (isNewCreated || !hasData) {
                System.out.println("【初始化】检测到知识库为空，准备导入数据...");
                ingestMedicalDocuments();
            } else {
                System.out.println("【初始化】检测到 Qdrant 中已有医疗知识，跳过自动导入。✅");
            }

        } catch (Exception e) {
            System.err.println("【初始化失败】原因: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void ingestMedicalDocuments() {
        System.out.println("【RAG系统】开始扫描知识库目录: " + knowledgePath);

        File directory = new File(knowledgePath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("【错误】知识库目录不存在！路径: " + knowledgePath);
            return;
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("【提示】目录为空，无需导入。");
            return;
        }

        List<Document> allDocuments = new ArrayList<>();

        // 统一使用 UTF-8 解析文本（建议将所有 txt/md 文件另存为 UTF-8 编码）
        DocumentParser utf8TextParser = new TextDocumentParser(StandardCharsets.UTF_8);
        DocumentParser pdfParser = new ApachePdfBoxDocumentParser();

        for (File file : files) {
            String fileName = file.getName().toLowerCase();
            Path filePath = file.toPath();

            try {
                Document doc = null;
                if (fileName.endsWith(".pdf")) {
                    System.out.println("--> 正在解析 PDF: " + fileName);
                    doc = FileSystemDocumentLoader.loadDocument(filePath, pdfParser);
                } else if (fileName.endsWith(".md") || fileName.endsWith(".txt")) {
                    System.out.println("--> 正在解析 文本: " + fileName);
                    doc = FileSystemDocumentLoader.loadDocument(filePath, utf8TextParser);
                } else {
                    continue;
                }

                if (doc != null) {
                    // 注入元数据
                    doc.metadata().put("file_name", file.getName());
                    allDocuments.add(doc);
                }
            } catch (Exception e) {
                System.err.println("解析文件失败 [" + fileName + "]: " + e.getMessage());
            }
        }

        if (allDocuments.isEmpty()) {
            System.out.println("【提示】没有提取到任何文档内容。");
            return;
        }

        // 3. 执行向量化存储
        System.out.println("【向量化】正在将 " + allDocuments.size() + " 份文档存入 Qdrant...");

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(DocumentSplitters.recursive(500, 50))
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest(allDocuments);
        System.out.println("【成功】所有文档已成功向量化并存入 Qdrant！");
    }
}