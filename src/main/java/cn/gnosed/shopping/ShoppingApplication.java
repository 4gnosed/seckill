package cn.gnosed.shopping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author: LuDeSong
 * @Date: 2020-7-22 14:54
 * @Description:
 */
@SpringBootApplication
@MapperScan("cn.gnosed.shopping.mapper")
public class ShoppingApplication {
    public static void main(String[] args) {
        SpringApplication.run(ShoppingApplication.class, args);
    }
}
