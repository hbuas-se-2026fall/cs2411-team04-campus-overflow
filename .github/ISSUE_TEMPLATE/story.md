---
name: 用户故事 / 任务卡
about: 一个可认领、可验收的工作单元
title: "[任务] "
labels: type:story
---

## 用户故事

作为 <角色>，我希望 <功能>，以便 <价值>。

## 验收标准

- [ ] Given … When … Then …
- [ ] 异常场景：… 返回 <状态码> <错误码>

## 实现落点

- 涉及方法：`Question.post()`、`QuestionCommandService.post()`
- 参考测试：`QuestionTest.Posting` 必须变绿

## 完成定义（DoD）

- [ ] 代码经 PR 合入 `develop`，至少 1 名非作者 Approve
- [ ] 相关测试全绿，未修改任何已有断言
- [ ] `ArchitectureTest` 通过
- [ ] PR 的 Checks 全绿
- [ ] 已更新本人 `docs/progress/<用户名>.md`
- [ ] 涉及接口或界面时，附调用截图

Closes #
