# ZenWealth 项目上下文

## 项目位置
`D:\WHR\homework\2\java\ks3`

## 运行方式
- IDEA: Maven面板 → Plugins → javafx → javafx:run
- 终端: `mvn javafx:run`
- IDEA 绿色按钮需配 Maven Run Config（Command line: `javafx:run`），因为 JDK21 不内置 JavaFX

## 技术栈
- Java 21, JavaFX 21.0.6, Maven 3.9, OkHttp 4.12, Gson 2.10, Apache POI 5.2.5, JUnit 5
- Tushare Pro API (token: 086be54688a4f8cb82a63ebe683a82a48866a02a5369ede8eee4503b)
- 数据文件: assets.json (自动生成), GPLIST.xls (1704只A股)

## 包结构
```
com.zenwealth/
├── ZenWealthApp.java              # 启动入口 extends Application
├── controller/
│   ├── MainController.java        # 主界面：侧边栏+表格+堆叠条+增删改刷新
│   └── DialogController.java      # 新增/编辑弹窗，含股票模糊搜索
├── model/
│   ├── AssetType.java             # CASH/DEPOSIT/STOCK/GOLD/BOND
│   ├── Asset.java                 # 抽象基类 name+type+getMarketValue()
│   ├── CashAsset.java             # name+amount
│   ├── DepositAsset.java          # name+amount+interestRate
│   ├── EquityAsset.java           # name+code+quantity+unitPrice (用于STOCK/GOLD/BOND)
│   ├── Portfolio.java             # 资产容器，getTotalMarketValue(), getCategoryRatios()
│   └── JsonPortfolio.java         # Gson序列化壳 (version+assets)
├── service/
│   ├── TushareService.java        # Tushare HTTP客户端，daily/fund_daily接口
│   ├── PriceRefreshService.java   # 遍历EquityAsset调API更新unitPrice
│   └── StorageManager.java        # JSON读写 + Gson自定义反序列化(AssetDeserializer)
├── ui/
│   └── StackedBar.java            # 横向堆叠占比条，彩色段+内嵌标签+总资产
└── util/
    ├── Constants.java              # token/API_URL/DATA_FILE
    └── StockDatabase.java          # 加载GPLIST.xls，search(keyword)模糊匹配
```

## 关键设计决策
1. **资产分类**: CASH和DEPOSIT共享25%目标，STOCK/GOLD/BOND各25%
2. **黄金**: 固定代码518880.SH(华安黄金ETF)，DialogController中锁定不可编辑，API用fund_daily
3. **股票**: 输入框触发StockDatabase.search()模糊搜索(名称/代码)，内嵌ListView显示结果(StackPanel overlay)，选中后自动填代码+名称，数据库校验通过才能添加
4. **持久化**: 每次增删改刷新后自动save到assets.json，启动时load恢复
5. **刷新**: 后台线程遍历EquityAsset调Tushare API，Platform.runLater更新UI
6. **Tushare**: STOCK用daily接口(close价)，GOLD/BOND用fund_daily接口(close价)，先检查response.has("data")防止JsonNull崩溃，检查code!=0判断业务错误

## 已修复的bug
- dialog.fxml 根GridPane缺少fx:id="formGrid"→对话框空内容
- onTypeChanged()未调hideAll()→切换类型时字段残留
- resultConverter返回null对话框直接关闭→改为addEventFilter+event.consume()
- PropertyValueFactory读不到Asset.name→改为显式cellValueFactory
- StockDatabase列检测错误(中文表头匹配失败)→改为匹配"证券简称"+"A股代码"且排除"B股"
- StockDatabase文件路径相对路径找不到→改为多候选路径
- Tushare黄金API名futures_daily→fut_daily→最终fund_daily
- Tushare返回data为null时getAsJsonObject()抛ClassCastException→先检查isJsonNull
- 搜索Popup定位失败→改为StackPane overlay内嵌ListView
- UI不够图形化→侧边栏彩色圆点+金额，头部渐变+阴影，堆叠条内嵌标签

## assets.json格式
```json
{
  "version": 1,
  "assets": [
    {"type":"CASH","name":"活期","amount":50000.0},
    {"type":"DEPOSIT","name":"一年定存","amount":100000.0,"interestRate":1.75},
    {"type":"STOCK","name":"贵州茅台","code":"600519.SH","quantity":100,"unitPrice":1500.0},
    {"type":"GOLD","name":"黄金ETF","code":"518880.SH","quantity":100,"unitPrice":5.5},
    {"type":"BOND","name":"国债ETF","code":"511010.SH","quantity":1000,"unitPrice":102.3}
  ]
}
```
