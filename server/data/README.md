# SoundZone 本地公版曲库

这个目录用于开发机上的可控演示曲库，不依赖 Audius：

- `audio/`：放 MP3 等音频文件。
- `covers/`：放 JPG、PNG 或 WebP 封面。
- `catalog.json`：登记曲目元数据与授权来源；可复制 `catalog.example.json` 开始填写。

音频和封面文件默认不提交到 Git。每首曲目必须登记授权引用、时长和合法标签，后端启动时会校验；文件不存在、超过 10 分钟、缺少授权信息或标签未知时会拒绝启动。

公开部署时把同样的 `audio/`、`covers/` 和 `catalog.json` 放到服务器项目根目录的 `data/`。部署脚本会保留这个目录，不会用仓库内容覆盖它。
