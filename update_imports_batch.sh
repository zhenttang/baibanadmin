#!/bin/bash
# 批量更新import语句脚本

BASE_DIR="/mnt/d/Documents/yunkebaiban/baibanhouduan/yunke-java-backend/src/main/java/com/yunke/backend"

echo "开始批量更新import语句..."

# Document模块
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocHistory;/import com.yunke.backend.document.domain.entity.DocHistory;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocRecord;/import com.yunke.backend.document.domain.entity.DocRecord;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocRole;/import com.yunke.backend.document.domain.entity.DocRole;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocMode;/import com.yunke.backend.document.domain.entity.DocMode;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocUpdate;/import com.yunke.backend.document.domain.entity.DocUpdate;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocumentCategory;/import com.yunke.backend.document.domain.entity.DocumentCategory;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocumentCollection;/import com.yunke.backend.document.domain.entity.DocumentCollection;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocumentComment;/import com.yunke.backend.document.domain.entity.DocumentComment;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocumentLike;/import com.yunke.backend.document.domain.entity.DocumentLike;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocumentPurchase;/import com.yunke.backend.document.domain.entity.DocumentPurchase;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocumentReport;/import com.yunke.backend.document.domain.entity.DocumentReport;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocumentTag;/import com.yunke.backend.document.domain.entity.DocumentTag;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocumentTagRelation;/import com.yunke.backend.document.domain.entity.DocumentTagRelation;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.DocumentView;/import com.yunke.backend.document.domain.entity.DocumentView;/g' {} \;

# Payment模块
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.PaymentRecord;/import com.yunke.backend.payment.domain.entity.PaymentRecord;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.PaymentStatus;/import com.yunke.backend.payment.domain.entity.PaymentStatus;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.Subscription;/import com.yunke.backend.payment.domain.entity.Subscription;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.Invoice;/import com.yunke.backend.payment.domain.entity.Invoice;/g' {} \;

# AI模块
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.AiSession;/import com.yunke.backend.ai.domain.entity.AiSession;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.AiSessionMessage;/import com.yunke.backend.ai.domain.entity.AiSessionMessage;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.CopilotSession;/import com.yunke.backend.ai.domain.entity.CopilotSession;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.CopilotMessage;/import com.yunke.backend.ai.domain.entity.CopilotMessage;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.CopilotQuota;/import com.yunke.backend.ai.domain.entity.CopilotQuota;/g' {} \;

# Community模块
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.CommunityDocument;/import com.yunke.backend.community.domain.entity.CommunityDocument;/g' {} \;

# Forum模块
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.Forum;/import com.yunke.backend.forum.domain.entity.Forum;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.ForumPost;/import com.yunke.backend.forum.domain.entity.ForumPost;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.ForumReply;/import com.yunke.backend.forum.domain.entity.ForumReply;/g' {} \;

# Storage模块
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.Blob;/import com.yunke.backend.storage.domain.entity.Blob;/g' {} \;

# Notification模块
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.Notification;/import com.yunke.backend.notification.domain.entity.Notification;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.MailQueue;/import com.yunke.backend.notification.domain.entity.MailQueue;/g' {} \;
find "$BASE_DIR" -name "*.java" -type f -exec sed -i 's/import com\.yunke\.backend\.entity\.MailTemplate;/import com.yunke.backend.notification.domain.entity.MailTemplate;/g' {} \;

echo "Import更新完成！"

