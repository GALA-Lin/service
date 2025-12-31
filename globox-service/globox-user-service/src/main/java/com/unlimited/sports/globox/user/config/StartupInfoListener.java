package com.unlimited.sports.globox.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * 启动成功提示监听器
 *
 * @author Wreckloud
 * @since 2025/12/19
 */
@Component
@Slf4j
public class StartupInfoListener implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${server.port:12024}")
    private String serverPort;

    @Value("${spring.application.name:globox-user-service}")
    private String applicationName;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Value("${springdoc.swagger-ui.path:/swagger-ui/index.html}")
    private String swaggerUiPath;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 获取服务器IP地址
        String serverIp = getServerIp();
        
        // 构建基础URL
        String baseUrl = "http://" + serverIp + ":" + serverPort;
        
        // 处理contextPath用于显示
        String displayContextPath = (contextPath == null || contextPath.isEmpty()) ? "" : contextPath;
        
        // 构建完整的Swagger URL
        String swaggerUrl = buildSwaggerUrl(baseUrl);
        
        log.info("");
        log.info("=================================================");
        log.info("   {} 启动成功！", applicationName);
        log.info("   Swagger API文档：{}", swaggerUrl);
        log.info("=================================================");
        log.info("");
    }

    /**
     * 获取服务器IP地址
     * 优先返回非回环地址的IP，如果获取失败则返回localhost
     */
    private String getServerIp() {
        try {
            // 优先尝试获取本机IP地址
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                // 跳过回环接口和未启用的接口
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    // 跳过回环地址和IPv6地址
                    if (address.isLoopbackAddress() || address.getHostAddress().contains(":")) {
                        continue;
                    }
                    String ip = address.getHostAddress();
                    // 返回第一个有效的非回环IPv4地址
                    if (ip != null && !ip.isEmpty()) {
                        return ip;
                    }
                }
            }
            
            // 如果上述方法失败，尝试使用InetAddress.getLocalHost()
            InetAddress localhost = InetAddress.getLocalHost();
            String ip = localhost.getHostAddress();
            if (ip != null && !ip.isEmpty() && !ip.equals("127.0.0.1")) {
                return ip;
            }
        } catch (SocketException | java.net.UnknownHostException e) {
            log.debug("获取服务器IP地址失败，使用localhost: {}", e.getMessage());
        }
        
        // 如果所有方法都失败，返回localhost
        return "localhost";
    }

    /**
     * 构建Swagger URL
     */
    private String buildSwaggerUrl(String baseUrl) {
        // 处理contextPath：如果为空或null，则设为空字符串；否则确保以/开头
        String path = (contextPath == null || contextPath.isEmpty()) ? "" : 
                      (contextPath.startsWith("/") ? contextPath : "/" + contextPath);
        
        // 确保swaggerUiPath以/开头
        String swaggerPath = (swaggerUiPath == null || swaggerUiPath.isEmpty()) ? "/swagger-ui/index.html" :
                            (swaggerUiPath.startsWith("/") ? swaggerUiPath : "/" + swaggerUiPath);
        
        return baseUrl + path + swaggerPath;
    }
}
