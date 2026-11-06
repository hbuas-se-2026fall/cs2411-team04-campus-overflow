# Roo Code 项目规则

开工前：

1. 读 `docs/status.md`（当前批次与门禁）。
2. 读 `docs/progress/<当前用户 GitHub 用户名>.md`。
3. 新建分支，不直接改 `main` / `develop`。

完工后：

1. 更新**本人**的 `docs/progress/<用户名>.md`。
2. 不改别人的文件，不改 `docs/status.md`。
3. PR 描述写 `Closes #<Issue号>`。

红线：不改已有测试断言、不删测试、不加 `@Disabled`；领域层不依赖 Spring/JPA/Jackson。

完整约定见 `CLAUDE.md`。
