package com.yunke.backend.document.domain.entity;

/**
 * 文档导出格式枚举
 */
public enum DocExportFormat {
    HTML,       // HTML格式
    PDF,        // PDF格式
    MARKDOWN,   // Markdown格式
    TEXT,       // 纯文本格式
    DOCX,       // Word文档格式
    PNG,        // PNG图片格式
    JPEG,       // JPEG图片格式
    SVG,        // SVG矢量图格式
    JSON;       // JSON格式

    /**
     * 获取文件扩展名
     */
    public String getExtension() {
        switch (this) {
            case PDF:
                return "pdf";
            case HTML:
                return "html";
            case MARKDOWN:
                return "md";
            case DOCX:
                return "docx";
            case TEXT:
                return "txt";
            case PNG:
                return "png";
            case JPEG:
                return "jpg";
            case SVG:
                return "svg";
            case JSON:
                return "json";
            default:
                return "txt";
        }
    }
    
    /**
     * 获取内容类型
     */
    public String getContentType() {
        switch (this) {
            case PDF:
                return "application/pdf";
            case HTML:
                return "text/html";
            case MARKDOWN:
                return "text/markdown";
            case DOCX:
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case TEXT:
                return "text/plain";
            case PNG:
                return "image/png";
            case JPEG:
                return "image/jpeg";
            case SVG:
                return "image/svg+xml";
            case JSON:
                return "application/json";
            default:
                return "text/plain";
        }
    }
} 