package github.javaguide.requestIdGenerate;

import github.javaguide.extension.SPI;

/**
 * @author gusang
 * @createTime on 2024/6/29
 */

@SPI
public interface RequestIdGenerate {
    String getRequestId();
}
