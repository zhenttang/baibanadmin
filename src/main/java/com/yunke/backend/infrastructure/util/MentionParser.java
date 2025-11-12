package com.yunke.backend.infrastructure.util;

import com.yunke.backend.forum.dto.MentionDTO;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析内容中的 @提及 并映射到用户ID
 */
@Component
@RequiredArgsConstructor
public class MentionParser {

    // 正则匹配: @[\u4e00-\u9fa5a-zA-Z0-9_]{2,20}
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("@([\\u4e00-\\u9fa5a-zA-Z0-9_]{2,20})");

    private final UserRepository userRepository;

    /**
     * 解析并返回有效的@用户（用户存在，且非自己），按出现顺序去重
     *
     * @param content       文本内容
     * @param currentUserId 当前用户ID（字符串），用于过滤掉@自己
     * @return 被@用户信息列表
     */
    public List<MentionDTO> parseMentions(String content, String currentUserId) {
        List<MentionDTO> results = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return results;
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        Set<String> uniqueUsernames = new LinkedHashSet<>();
        while (matcher.find()) {
            String username = matcher.group(1);
            if (username != null && !username.isBlank()) {
                uniqueUsernames.add(username);
            }
        }

        for (String username : uniqueUsernames) {
            Optional<User> uOpt = userRepository.findByName(username);
            if (uOpt.isEmpty()) {
                continue; // 用户不存在，跳过
            }
            User u = uOpt.get();
            if (u.getId() != null && u.getId().equals(currentUserId)) {
                continue; // 不能@自己
            }
            results.add(new MentionDTO(u.getId(), u.getName()));
        }

        return results;
    }
}

