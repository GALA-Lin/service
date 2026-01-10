package com.unlimited.sports.globox.user.client;

import com.alibaba.fastjson2.JSON;
import com.qcloud.cos.auth.COSSigner;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.unlimited.sports.globox.common.exception.GloboxApplicationException;
import com.unlimited.sports.globox.common.prop.CosProperties;
import com.unlimited.sports.globox.common.result.UserAuthCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 人像抠图客户端
 * 直接调用腾讯云数据万象 CI - AIPortraitMatting 接口
 * 使用上传时处理方式（PUT 请求）
 * 使用 COS 签名机制进行认证
 */
@Slf4j
@Component
public class PortraitMattingClient {

    private static final String CI_URL_PATTERN = "https://%s.cos.%s.myqcloud.com/%s";
    private static final long SIGN_EXPIRATION_TIME = 3600L; // 签名有效期 1 小时

    @Autowired
    private CosProperties cosProperties;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 上传文件并执行人像抠图
     *
     * @param filePath 文件存储路径
     * @param fileContent 文件内容（字节）
     * @return 抠图后图片的URL
     */
    public String uploadAndMatting(String filePath, byte[] fileContent) {
        try {
            String url = buildUrl(filePath);

            // 构建 Pic-Operations 请求头
            String picOperations = buildPicOperations(filePath);

            // 构建请求头（包含签名所需的所有头部）
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(fileContent.length);

            // 添加 Host 头（必须）
            String host = buildHost();
            headers.set("Host", host);

            // 添加 Pic-Operations 头（需要被签入签名）
            headers.set("Pic-Operations", picOperations);

            // 生成签名（此时 headers 中包含签名所需的所有头）
            String authorization = generateSignature(HttpMethodName.PUT, filePath, headers);
            headers.set("Authorization", authorization);

            // 构建请求
            HttpEntity<byte[]> entity = new HttpEntity<>(fileContent, headers);

            // 发送 PUT 请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("上传并抠图失败: statusCode={}, body={}", response.getStatusCode(), response.getBody());
                throw new GloboxApplicationException(UserAuthCode.PORTRAIT_MATTING_FAILED);
            }

            // 解析响应 XML 获取处理后的图片 URL
            String mattingUrl = parseResponseXml(response.getBody(), filePath);

            log.info("人像抠图成功: filePath={}, mattingUrl={}", filePath, mattingUrl);
            return mattingUrl;

        } catch (GloboxApplicationException e) {
            log.error("人像抠图业务异常: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("人像抠图异常: filePath={}", filePath, e);
            throw new GloboxApplicationException(UserAuthCode.PORTRAIT_MATTING_FAILED);
        }
    }

    /**
     * 生成 COS 签名
     *
     * @param httpMethod HTTP 方法
     * @param resourcePath 资源路径
     * @param headers 请求头
     * @return Authorization 签名字符串
     */
    private String generateSignature(HttpMethodName httpMethod, String resourcePath, HttpHeaders headers) {
        try {
            // 创建 COS 凭证
            COSCredentials credentials = new BasicCOSCredentials(
                    cosProperties.getSecretId(),
                    cosProperties.getSecretKey()
            );

            // 计算签名有效期
            long currentTime = System.currentTimeMillis() / 1000;
            Date startTime = new Date(currentTime * 1000);
            Date expiredTime = new Date((currentTime + SIGN_EXPIRATION_TIME) * 1000);

            // 确保 pic-operations 在需要签名的头部集合中
            // COSSigner 有一个静态的 needSignedHeaderSet，确保它包含 pic-operations
            COSSigner.getNeedSignedHeaderSet().add("pic-operations");

            // 创建签名器
            COSSigner signer = new COSSigner();

            // 将 HttpHeaders 转换为 Map
            Map<String, String> headerMap = new HashMap<>();
            headers.forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    headerMap.put(key.toLowerCase(), values.get(0));
                }
            });

            // 生成签名
            String signature = signer.buildAuthorizationStr(
                    httpMethod,
                    "/" + resourcePath,
                    headerMap,
                    new HashMap<>(),
                    credentials,
                    startTime,
                    expiredTime,
                    true
            );

            return signature;

        } catch (Exception e) {
            log.error("生成签名失败", e);
            throw new GloboxApplicationException(UserAuthCode.PORTRAIT_MATTING_FAILED);
        }
    }

    /**
     * 构建请求 URL
     *
     * @param filePath 文件路径
     * @return 完整 URL
     */
    private String buildUrl(String filePath) {
        return String.format(CI_URL_PATTERN,
                cosProperties.getBucketName(),
                cosProperties.getRegion(),
                filePath);
    }

    /**
     * 构建 Host 头
     *
     * @return Host 头值
     */
    private String buildHost() {
        return String.format("%s.cos.%s.myqcloud.com",
                cosProperties.getBucketName(),
                cosProperties.getRegion());
    }

    /**
     * 构建 Pic-Operations 请求头
     * JSON 格式：{"is_pic_info": 1, "rules": [{"fileid": "path", "rule": "ci-process=AIPortraitMatting"}]}
     *
     * @param filePath 文件路径
     * @return JSON 字符串
     */
    private String buildPicOperations(String filePath) {
        Map<String, Object> operations = new HashMap<>();
        operations.put("is_pic_info", 1);

        Map<String, Object> rule = new HashMap<>();
        rule.put("fileid", filePath);
        rule.put("rule", "ci-process=AIPortraitMatting");

        Map<String, Object>[] rules = new Map[]{rule};
        operations.put("rules", rules);

        return JSON.toJSONString(operations);
    }

    /**
     * 解析响应 XML 获取处理后的图片 URL
     *
     * @param xmlResponse XML 响应字符串
     * @param originalFilePath 原始文件路径（用于日志记录）
     * @return 抠图后的图片 URL
     * @throws GloboxApplicationException 如果解析失败或响应中没有有效的 Location
     */
    private String parseResponseXml(String xmlResponse, String originalFilePath) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlResponse)));

            // 查找 ProcessResults -> Object -> Location
            NodeList objectNodes = document.getElementsByTagName("Object");
            if (objectNodes.getLength() > 0) {
                Element objectElement = (Element) objectNodes.item(0);
                NodeList locationNodes = objectElement.getElementsByTagName("Location");
                if (locationNodes.getLength() > 0) {
                    String location = locationNodes.item(0).getTextContent();
                    if (location != null && !location.isEmpty()) {
                        log.info("成功解析抠图结果 URL: {}", location);
                        return location;
                    }
                }
            }

            log.error("响应 XML 中未找到有效的 Location: filePath={}, response={}", originalFilePath, xmlResponse);
            throw new GloboxApplicationException(UserAuthCode.PORTRAIT_MATTING_FAILED);

        } catch (GloboxApplicationException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 XML 响应异常: filePath={}", originalFilePath, e);
            throw new GloboxApplicationException(UserAuthCode.PORTRAIT_MATTING_FAILED);
        }
    }
}
