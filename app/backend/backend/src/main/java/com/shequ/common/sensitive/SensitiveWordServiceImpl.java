package com.shequ.common.sensitive;

import com.shequ.common.exception.BusinessException;
import com.shequ.common.result.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 敏感词过滤最简实现：内置一份演示词表 + 支持配置追加。
 * <p>
 * 命中即抛 40002（message 提示修改后重新发布）。词表规模当前很小，
 * 用 {@code String.contains} 顺序匹配即可；接入 DB 词表或第三方服务时替换本实现。
 */
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    /** 内置演示词表（后续接入 sensitive_word 表 / 第三方内容安全服务后替换）。 */
    private static final List<String> DEFAULT_WORDS = List.of(
            "赌博", "博彩", "色情", "诈骗", "刷单", "办证", "代开发票", "违禁品");

    private final List<String> words;

    public SensitiveWordServiceImpl(@Value("${sensitive.extra-words:}") String extraWords) {
        List<String> all = new ArrayList<>(DEFAULT_WORDS);
        if (StringUtils.hasText(extraWords)) {
            for (String word : extraWords.split(",")) {
                String trimmed = word.trim();
                if (!trimmed.isEmpty() && !all.contains(trimmed)) {
                    all.add(trimmed);
                }
            }
        }
        this.words = List.copyOf(all);
    }

    @Override
    public void check(String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }
        for (String word : words) {
            if (text.contains(word)) {
                throw new BusinessException(ErrorCode.SENSITIVE_WORD, "内容包含敏感词「" + word + "」，请修改后重新发布");
            }
        }
    }

    @Override
    public boolean contains(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

}
