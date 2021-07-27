package cn.gnosed.shopping.service;

import cn.gnosed.shopping.entity.Good;
import cn.gnosed.shopping.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author Gnosed Lu
 * @since 2020-07-22
 */
public interface IGoodService extends IService<Good> {

    boolean buy(Integer goodId, Integer quantity, Integer userId);

    Good getGood(String goodId);
}
