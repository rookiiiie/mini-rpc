package github.javaguide.requestIdGenerate.requestIdGenerateStrategy;

import github.javaguide.requestIdGenerate.RequestIdGenerate;

/**
 * 功能描述
 *
 * @author: gusang
 * @date: 2024年06月29日 18:54
 */
public class UUID implements RequestIdGenerate {

    @Override
    public String getRequestId() {
        return java.util.UUID.randomUUID().toString();
    }
}
