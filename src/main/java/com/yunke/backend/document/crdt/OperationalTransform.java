package com.yunke.backend.document.crdt;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作变换引擎 - 实现完整的OT算法
 * 
 * 完全对应YJS的Operational Transformation实现
 * 处理并发操作的冲突解决和因果一致性
 */
@Slf4j
public class OperationalTransform {
    
    // 操作历史记录，用于变换计算
    private final Map<String, List<Operation>> operationHistory = new ConcurrentHashMap<>();
    
    // 状态向量缓存
    private final Map<String, StateVector> stateVectorCache = new ConcurrentHashMap<>();
    
    // 变换矩阵缓存
    private final Map<String, TransformResult> transformCache = new ConcurrentHashMap<>();
    
    /**
     * 变换结果
     */
    public static class TransformResult {
        private final Operation transformedOp1;
        private final Operation transformedOp2;
        private final String reason;
        private final long computeTime;
        
        public TransformResult(Operation transformedOp1, Operation transformedOp2, String reason) {
            this.transformedOp1 = transformedOp1;
            this.transformedOp2 = transformedOp2;
            this.reason = reason;
            this.computeTime = System.currentTimeMillis();
        }
        
        // Getters
        public Operation getTransformedOp1() { return transformedOp1; }
        public Operation getTransformedOp2() { return transformedOp2; }
        public String getReason() { return reason; }
        public long getComputeTime() { return computeTime; }
    }
    
    /**
     * 变换两个并发操作
     * 这是OT算法的核心函数
     */
    public TransformResult transform(Operation op1, Operation op2, boolean op1HasPriority) {
        // 生成缓存键
        String cacheKey = generateCacheKey(op1, op2, op1HasPriority);
        
        // 检查缓存
        TransformResult cached = transformCache.get(cacheKey);
        if (cached != null) {
            log.debug("Using cached transform result for operations: {} and {}", op1.getId(), op2.getId());
            return cached;
        }
        
        TransformResult result = computeTransform(op1, op2, op1HasPriority);
        
        // 缓存结果
        transformCache.put(cacheKey, result);
        
        log.debug("Transformed operations: {} -> {}, {} -> {}, reason: {}", 
                 op1.getId(), result.getTransformedOp1().getId(),
                 op2.getId(), result.getTransformedOp2().getId(),
                 result.getReason());
        
        return result;
    }
    
    /**
     * 计算操作变换
     */
    private TransformResult computeTransform(Operation op1, Operation op2, boolean op1HasPriority) {
        // 检查操作是否作用于同一对象
        if (!Objects.equals(op1.getTypeName(), op2.getTypeName()) || 
            !Objects.equals(op1.getParentId(), op2.getParentId())) {
            // 不同对象的操作不需要变换
            return new TransformResult(op1, op2, "Different targets - no transformation needed");
        }
        
        // 根据操作类型进行变换
        switch (op1.getType()) {
            case INSERT:
                return transformInsert(op1, op2, op1HasPriority);
            case DELETE:
                return transformDelete(op1, op2, op1HasPriority);
            case FORMAT:
                return transformFormat(op1, op2, op1HasPriority);
            case RETAIN:
                return transformRetain(op1, op2, op1HasPriority);
            default:
                return new TransformResult(op1, op2, "Unsupported operation type");
        }
    }
    
    /**
     * 变换插入操作
     */
    private TransformResult transformInsert(Operation op1, Operation op2, boolean op1HasPriority) {
        switch (op2.getType()) {
            case INSERT:
                return transformInsertInsert(op1, op2, op1HasPriority);
            case DELETE:
                return transformInsertDelete(op1, op2);
            case FORMAT:
                return transformInsertFormat(op1, op2);
            case RETAIN:
                return transformInsertRetain(op1, op2);
            default:
                return new TransformResult(op1, op2, "Unsupported operation combination");
        }
    }
    
    /**
     * 变换两个插入操作 (INSERT-INSERT)
     */
    private TransformResult transformInsertInsert(Operation insert1, Operation insert2, boolean op1HasPriority) {
        int pos1 = insert1.getIndex();
        int pos2 = insert2.getIndex();
        
        if (pos1 < pos2) {
            // insert1在insert2之前，insert2需要向后偏移
            Operation newInsert2 = createShiftedInsert(insert2, insert1.getContentLength());
            return new TransformResult(insert1, newInsert2, "INSERT-INSERT: first operation comes before second");
        } else if (pos1 > pos2) {
            // insert2在insert1之前，insert1需要向后偏移
            Operation newInsert1 = createShiftedInsert(insert1, insert2.getContentLength());
            return new TransformResult(newInsert1, insert2, "INSERT-INSERT: second operation comes before first");
        } else {
            // 同一位置插入，使用优先级解决冲突
            if (op1HasPriority || (insert1.getClock() < insert2.getClock()) || 
                (insert1.getClock() == insert2.getClock() && 
                 insert1.getClientId().compareTo(insert2.getClientId()) < 0)) {
                // insert1有优先权，insert2向后偏移
                Operation newInsert2 = createShiftedInsert(insert2, insert1.getContentLength());
                return new TransformResult(insert1, newInsert2, "INSERT-INSERT: first operation has priority");
            } else {
                // insert2有优先权，insert1向后偏移
                Operation newInsert1 = createShiftedInsert(insert1, insert2.getContentLength());
                return new TransformResult(newInsert1, insert2, "INSERT-INSERT: second operation has priority");
            }
        }
    }
    
    /**
     * 变换插入和删除操作 (INSERT-DELETE)
     */
    private TransformResult transformInsertDelete(Operation insert, Operation delete) {
        int insertPos = insert.getIndex();
        int deletePos = delete.getIndex();
        int deleteLen = delete.getContentLength();
        
        if (insertPos <= deletePos) {
            // 插入在删除范围之前或开始处，删除位置需要向后偏移
            Operation newDelete = createShiftedDelete(delete, insert.getContentLength());
            return new TransformResult(insert, newDelete, "INSERT-DELETE: insert before delete range");
        } else if (insertPos >= deletePos + deleteLen) {
            // 插入在删除范围之后，插入位置需要向前偏移
            Operation newInsert = createShiftedInsert(insert, -deleteLen);
            return new TransformResult(newInsert, delete, "INSERT-DELETE: insert after delete range");
        } else {
            // 插入在删除范围内，插入位置调整到删除开始位置
            Operation newInsert = Operation.createInsert(
                insert.getClientId(), insert.getClock(), insert.getTypeName(),
                insert.getParentId(), insert.getKey(), deletePos, 
                insert.getContent(), insert.getOrigin()
            );
            return new TransformResult(newInsert, delete, "INSERT-DELETE: insert within delete range");
        }
    }
    
    /**
     * 变换插入和格式化操作 (INSERT-FORMAT)
     */
    private TransformResult transformInsertFormat(Operation insert, Operation format) {
        int insertPos = insert.getIndex();
        int formatPos = format.getIndex();
        int formatLen = format.getContentLength();
        
        if (insertPos <= formatPos) {
            // 插入在格式化范围之前，格式化位置向后偏移
            Operation newFormat = createShiftedFormat(format, insert.getContentLength());
            return new TransformResult(insert, newFormat, "INSERT-FORMAT: insert before format range");
        } else if (insertPos >= formatPos + formatLen) {
            // 插入在格式化范围之后，不需要变换
            return new TransformResult(insert, format, "INSERT-FORMAT: insert after format range");
        } else {
            // 插入在格式化范围内，格式化长度需要增加
            Operation newFormat = createExtendedFormat(format, insert.getContentLength());
            return new TransformResult(insert, newFormat, "INSERT-FORMAT: insert within format range");
        }
    }
    
    /**
     * 变换插入和保留操作 (INSERT-RETAIN)
     */
    private TransformResult transformInsertRetain(Operation insert, Operation retain) {
        int insertPos = insert.getIndex();
        int retainLen = retain.getContentLength();
        
        if (insertPos == 0) {
            // 在开始处插入，保留长度增加
            Operation newRetain = Operation.createRetain(
                retain.getClientId(), retain.getClock(), retain.getTypeName(),
                retainLen + insert.getContentLength(), retain.getAttributes(), retain.getOrigin()
            );
            return new TransformResult(insert, newRetain, "INSERT-RETAIN: insert at beginning");
        } else {
            // 其他位置插入，可能需要分割保留操作
            return new TransformResult(insert, retain, "INSERT-RETAIN: no transformation needed");
        }
    }
    
    /**
     * 变换删除操作
     */
    private TransformResult transformDelete(Operation op1, Operation op2, boolean op1HasPriority) {
        switch (op2.getType()) {
            case INSERT:
                // DELETE-INSERT 是 INSERT-DELETE 的反向
                TransformResult reverseResult = transformInsertDelete(op2, op1);
                return new TransformResult(reverseResult.getTransformedOp2(), 
                                         reverseResult.getTransformedOp1(), 
                                         "DELETE-INSERT: " + reverseResult.getReason());
            case DELETE:
                return transformDeleteDelete(op1, op2, op1HasPriority);
            case FORMAT:
                return transformDeleteFormat(op1, op2);
            case RETAIN:
                return transformDeleteRetain(op1, op2);
            default:
                return new TransformResult(op1, op2, "Unsupported operation combination");
        }
    }
    
    /**
     * 变换两个删除操作 (DELETE-DELETE)
     */
    private TransformResult transformDeleteDelete(Operation delete1, Operation delete2, boolean op1HasPriority) {
        int pos1 = delete1.getIndex();
        int len1 = delete1.getContentLength();
        int pos2 = delete2.getIndex();
        int len2 = delete2.getContentLength();
        
        int end1 = pos1 + len1;
        int end2 = pos2 + len2;
        
        if (end1 <= pos2) {
            // delete1完全在delete2之前
            Operation newDelete2 = createShiftedDelete(delete2, -len1);
            return new TransformResult(delete1, newDelete2, "DELETE-DELETE: first completely before second");
        } else if (end2 <= pos1) {
            // delete2完全在delete1之前
            Operation newDelete1 = createShiftedDelete(delete1, -len2);
            return new TransformResult(newDelete1, delete2, "DELETE-DELETE: second completely before first");
        } else {
            // 删除范围重叠，需要计算重叠部分
            return transformOverlappingDeletes(delete1, delete2, op1HasPriority);
        }
    }
    
    /**
     * 变换重叠的删除操作
     */
    private TransformResult transformOverlappingDeletes(Operation delete1, Operation delete2, boolean op1HasPriority) {
        int pos1 = delete1.getIndex();
        int len1 = delete1.getContentLength();
        int pos2 = delete2.getIndex();
        int len2 = delete2.getContentLength();
        
        int start = Math.min(pos1, pos2);
        int end1 = pos1 + len1;
        int end2 = pos2 + len2;
        int end = Math.max(end1, end2);
        
        // 计算重叠区域
        int overlapStart = Math.max(pos1, pos2);
        int overlapEnd = Math.min(end1, end2);
        int overlapLen = Math.max(0, overlapEnd - overlapStart);
        
        if (overlapLen == 0) {
            // 实际上没有重叠（相邻）
            return new TransformResult(delete1, delete2, "DELETE-DELETE: adjacent ranges");
        }
        
        // 创建调整后的删除操作
        Operation newDelete1, newDelete2;
        
        if (pos1 <= pos2) {
            // delete1开始位置在前或相同
            if (op1HasPriority) {
                // delete1优先，delete2调整为删除剩余部分
                int newPos2 = pos1;
                int newLen2 = len2 - overlapLen;
                if (newLen2 > 0) {
                    newDelete2 = Operation.createDelete(
                        delete2.getClientId(), delete2.getClock(), delete2.getTypeName(),
                        delete2.getParentId(), delete2.getKey(), newPos2, newLen2, delete2.getOrigin()
                    );
                } else {
                    newDelete2 = null; // 删除操作被完全吸收
                }
                newDelete1 = delete1;
            } else {
                // delete2优先，delete1调整
                int newLen1 = len1 - overlapLen;
                if (newLen1 > 0) {
                    newDelete1 = Operation.createDelete(
                        delete1.getClientId(), delete1.getClock(), delete1.getTypeName(),
                        delete1.getParentId(), delete1.getKey(), pos1, newLen1, delete1.getOrigin()
                    );
                } else {
                    newDelete1 = null; // 删除操作被完全吸收
                }
                newDelete2 = delete2;
            }
        } else {
            // delete2开始位置在前
            if (op1HasPriority) {
                // delete1优先
                int newLen2 = len2 - overlapLen;
                if (newLen2 > 0) {
                    newDelete2 = Operation.createDelete(
                        delete2.getClientId(), delete2.getClock(), delete2.getTypeName(),
                        delete2.getParentId(), delete2.getKey(), pos2, newLen2, delete2.getOrigin()
                    );
                } else {
                    newDelete2 = null;
                }
                newDelete1 = Operation.createDelete(
                    delete1.getClientId(), delete1.getClock(), delete1.getTypeName(),
                    delete1.getParentId(), delete1.getKey(), pos2, len1, delete1.getOrigin()
                );
            } else {
                // delete2优先
                int newPos1 = pos2;
                int newLen1 = len1 - overlapLen;
                if (newLen1 > 0) {
                    newDelete1 = Operation.createDelete(
                        delete1.getClientId(), delete1.getClock(), delete1.getTypeName(),
                        delete1.getParentId(), delete1.getKey(), newPos1, newLen1, delete1.getOrigin()
                    );
                } else {
                    newDelete1 = null;
                }
                newDelete2 = delete2;
            }
        }
        
        // 处理null操作（被完全吸收的情况）
        if (newDelete1 == null) {
            newDelete1 = createNoOpOperation(delete1);
        }
        if (newDelete2 == null) {
            newDelete2 = createNoOpOperation(delete2);
        }
        
        return new TransformResult(newDelete1, newDelete2, "DELETE-DELETE: overlapping ranges resolved");
    }
    
    /**
     * 变换删除和格式化操作 (DELETE-FORMAT)
     */
    private TransformResult transformDeleteFormat(Operation delete, Operation format) {
        int deletePos = delete.getIndex();
        int deleteLen = delete.getContentLength();
        int formatPos = format.getIndex();
        int formatLen = format.getContentLength();
        
        int deleteEnd = deletePos + deleteLen;
        int formatEnd = formatPos + formatLen;
        
        if (deleteEnd <= formatPos) {
            // 删除在格式化之前，格式化位置前移
            Operation newFormat = createShiftedFormat(format, -deleteLen);
            return new TransformResult(delete, newFormat, "DELETE-FORMAT: delete before format");
        } else if (deletePos >= formatEnd) {
            // 删除在格式化之后，不需要变换
            return new TransformResult(delete, format, "DELETE-FORMAT: delete after format");
        } else {
            // 删除与格式化重叠，需要调整格式化范围
            int overlapStart = Math.max(deletePos, formatPos);
            int overlapEnd = Math.min(deleteEnd, formatEnd);
            int overlapLen = overlapEnd - overlapStart;
            
            int newFormatPos = Math.min(deletePos, formatPos);
            int newFormatLen = formatLen - overlapLen;
            
            if (newFormatLen > 0) {
                Operation newFormat = Operation.createFormat(
                    format.getClientId(), format.getClock(), format.getTypeName(),
                    format.getParentId(), newFormatPos, newFormatLen, 
                    format.getAttributes(), format.getOrigin()
                );
                return new TransformResult(delete, newFormat, "DELETE-FORMAT: overlapping ranges adjusted");
            } else {
                // 格式化范围被完全删除
                Operation noOpFormat = createNoOpOperation(format);
                return new TransformResult(delete, noOpFormat, "DELETE-FORMAT: format range completely deleted");
            }
        }
    }
    
    /**
     * 变换删除和保留操作 (DELETE-RETAIN)
     */
    private TransformResult transformDeleteRetain(Operation delete, Operation retain) {
        // 保留操作通常不受删除影响，但长度可能需要调整
        int deleteLen = delete.getContentLength();
        int retainLen = retain.getContentLength();
        
        if (retainLen > deleteLen) {
            Operation newRetain = Operation.createRetain(
                retain.getClientId(), retain.getClock(), retain.getTypeName(),
                retainLen - deleteLen, retain.getAttributes(), retain.getOrigin()
            );
            return new TransformResult(delete, newRetain, "DELETE-RETAIN: retain length adjusted");
        } else {
            return new TransformResult(delete, retain, "DELETE-RETAIN: no transformation needed");
        }
    }
    
    /**
     * 变换格式化操作
     */
    private TransformResult transformFormat(Operation op1, Operation op2, boolean op1HasPriority) {
        switch (op2.getType()) {
            case INSERT:
                // FORMAT-INSERT 是 INSERT-FORMAT 的反向
                TransformResult reverseResult = transformInsertFormat(op2, op1);
                return new TransformResult(reverseResult.getTransformedOp2(), 
                                         reverseResult.getTransformedOp1(), 
                                         "FORMAT-INSERT: " + reverseResult.getReason());
            case DELETE:
                // FORMAT-DELETE 是 DELETE-FORMAT 的反向
                TransformResult reverseResult2 = transformDeleteFormat(op2, op1);
                return new TransformResult(reverseResult2.getTransformedOp2(), 
                                         reverseResult2.getTransformedOp1(), 
                                         "FORMAT-DELETE: " + reverseResult2.getReason());
            case FORMAT:
                return transformFormatFormat(op1, op2, op1HasPriority);
            case RETAIN:
                return transformFormatRetain(op1, op2);
            default:
                return new TransformResult(op1, op2, "Unsupported operation combination");
        }
    }
    
    /**
     * 变换两个格式化操作 (FORMAT-FORMAT)
     */
    private TransformResult transformFormatFormat(Operation format1, Operation format2, boolean op1HasPriority) {
        int pos1 = format1.getIndex();
        int len1 = format1.getContentLength();
        int pos2 = format2.getIndex();
        int len2 = format2.getContentLength();
        
        int end1 = pos1 + len1;
        int end2 = pos2 + len2;
        
        // 检查是否有重叠
        if (end1 <= pos2 || end2 <= pos1) {
            // 没有重叠
            return new TransformResult(format1, format2, "FORMAT-FORMAT: no overlap");
        }
        
        // 有重叠，需要合并属性
        int overlapStart = Math.max(pos1, pos2);
        int overlapEnd = Math.min(end1, end2);
        
        // 合并属性（优先级高的覆盖低的）
        Map<String, Object> mergedAttributes = mergeAttributes(
            format1.getAttributes(), format2.getAttributes(), op1HasPriority
        );
        
        // 创建覆盖重叠区域的格式化操作
        Operation mergedFormat1 = Operation.createFormat(
            format1.getClientId(), format1.getClock(), format1.getTypeName(),
            format1.getParentId(), pos1, len1, mergedAttributes, format1.getOrigin()
        );
        
        Operation mergedFormat2 = Operation.createFormat(
            format2.getClientId(), format2.getClock(), format2.getTypeName(),
            format2.getParentId(), pos2, len2, mergedAttributes, format2.getOrigin()
        );
        
        return new TransformResult(mergedFormat1, mergedFormat2, "FORMAT-FORMAT: attributes merged");
    }
    
    /**
     * 变换格式化和保留操作 (FORMAT-RETAIN)
     */
    private TransformResult transformFormatRetain(Operation format, Operation retain) {
        // 格式化和保留操作通常不冲突
        return new TransformResult(format, retain, "FORMAT-RETAIN: no transformation needed");
    }
    
    /**
     * 变换保留操作
     */
    private TransformResult transformRetain(Operation op1, Operation op2, boolean op1HasPriority) {
        // 保留操作与其他操作的变换通常很简单
        switch (op2.getType()) {
            case INSERT:
                TransformResult reverseResult = transformInsertRetain(op2, op1);
                return new TransformResult(reverseResult.getTransformedOp2(), 
                                         reverseResult.getTransformedOp1(), 
                                         "RETAIN-INSERT: " + reverseResult.getReason());
            case DELETE:
                TransformResult reverseResult2 = transformDeleteRetain(op2, op1);
                return new TransformResult(reverseResult2.getTransformedOp2(), 
                                         reverseResult2.getTransformedOp1(), 
                                         "RETAIN-DELETE: " + reverseResult2.getReason());
            case FORMAT:
                TransformResult reverseResult3 = transformFormatRetain(op2, op1);
                return new TransformResult(reverseResult3.getTransformedOp2(), 
                                         reverseResult3.getTransformedOp1(), 
                                         "RETAIN-FORMAT: " + reverseResult3.getReason());
            case RETAIN:
                return transformRetainRetain(op1, op2);
            default:
                return new TransformResult(op1, op2, "Unsupported operation combination");
        }
    }
    
    /**
     * 变换两个保留操作 (RETAIN-RETAIN)
     */
    private TransformResult transformRetainRetain(Operation retain1, Operation retain2) {
        // 两个保留操作可以合并
        int len1 = retain1.getContentLength();
        int len2 = retain2.getContentLength();
        int totalLen = len1 + len2;
        
        // 合并属性
        Map<String, Object> mergedAttributes = mergeAttributes(
            retain1.getAttributes(), retain2.getAttributes(), true
        );
        
        Operation mergedRetain = Operation.createRetain(
            retain1.getClientId(), retain1.getClock(), retain1.getTypeName(),
            totalLen, mergedAttributes, retain1.getOrigin()
        );
        
        return new TransformResult(mergedRetain, createNoOpOperation(retain2), "RETAIN-RETAIN: merged");
    }
    
    // 辅助方法
    
    private Operation createShiftedInsert(Operation insert, int offset) {
        return Operation.createInsert(
            insert.getClientId(), insert.getClock(), insert.getTypeName(),
            insert.getParentId(), insert.getKey(), insert.getIndex() + offset,
            insert.getContent(), insert.getOrigin()
        );
    }
    
    private Operation createShiftedDelete(Operation delete, int offset) {
        return Operation.createDelete(
            delete.getClientId(), delete.getClock(), delete.getTypeName(),
            delete.getParentId(), delete.getKey(), delete.getIndex() + offset,
            delete.getContentLength(), delete.getOrigin()
        );
    }
    
    private Operation createShiftedFormat(Operation format, int offset) {
        return Operation.createFormat(
            format.getClientId(), format.getClock(), format.getTypeName(),
            format.getParentId(), format.getIndex() + offset, format.getContentLength(),
            format.getAttributes(), format.getOrigin()
        );
    }
    
    private Operation createExtendedFormat(Operation format, int additionalLength) {
        return Operation.createFormat(
            format.getClientId(), format.getClock(), format.getTypeName(),
            format.getParentId(), format.getIndex(), 
            format.getContentLength() + additionalLength,
            format.getAttributes(), format.getOrigin()
        );
    }
    
    private Operation createNoOpOperation(Operation original) {
        return Operation.createRetain(
            original.getClientId(), original.getClock(), original.getTypeName(),
            0, null, original.getOrigin()
        );
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeAttributes(Object attrs1, Object attrs2, boolean priority1) {
        Map<String, Object> result = new HashMap<>();
        
        if (attrs1 instanceof Map) {
            result.putAll((Map<String, Object>) attrs1);
        }
        
        if (attrs2 instanceof Map) {
            Map<String, Object> map2 = (Map<String, Object>) attrs2;
            if (priority1) {
                // attrs1优先，只添加attrs1中没有的属性
                for (Map.Entry<String, Object> entry : map2.entrySet()) {
                    result.putIfAbsent(entry.getKey(), entry.getValue());
                }
            } else {
                // attrs2优先，覆盖attrs1中的属性
                result.putAll(map2);
            }
        }
        
        return result;
    }
    
    private String generateCacheKey(Operation op1, Operation op2, boolean op1HasPriority) {
        return String.format("%s:%s:%s", op1.getId(), op2.getId(), op1HasPriority);
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        transformCache.clear();
        operationHistory.clear();
        stateVectorCache.clear();
        log.info("OT cache cleared");
    }
    
    /**
     * 获取缓存统计
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("transformCacheSize", transformCache.size());
        stats.put("operationHistorySize", operationHistory.size());
        stats.put("stateVectorCacheSize", stateVectorCache.size());
        return stats;
    }
}