# Stage 2 촬영 실행 대본

작성일 2026년 11월 14일. 아래 구간 번호는 촬영 계획이며 실제 녹화 타임스탬프가 아니다.

## 먼저 답

교수에게 보여줄 것은 실제 프로그램의 입력, 실행, 결과다. 얼굴 촬영이나 특정 목소리 형식은 요구되지 않는다. 기능 ID와 이름, 무엇을 하는 기능인지, AI가 관여하는 지점을 설명해야 한다. 짧은 영어 나레이션을 읽는 방식을 권장한다. 음성을 쓰기 어렵다면 아래 영어 문장을 자막으로 넣고 읽을 시간을 준다. 둘 다 할 필요는 없다.

지침은 영상 개수나 분량을 정하지 않는다. 여기서는 세 영상으로 나눈다. 전체 약 25~35분은 권장 촬영 분량이며 제한 시간이 아니다. 원본 PDF를 보여주기만 하거나 슬라이드로 설명하는 것은 실행 시연을 대체하지 못한다. 각 기능은 반드시 결과까지 보여준다.

## 준비와 실행

1. `codex/recording-app-20261114`는 녹화용 독립 실행 폴더다. 그 안의 **Study Agent.exe**를 더블클릭한다. GUI를 켜기 위해 PowerShell을 사용할 필요가 없다. CLI는 같은 폴더의 **Study Agent CLI.exe**를 더블클릭한다. CLI에 콘솔 창이 뜨는 것은 정상이다.
2. 녹화용 앱의 `data/`만 쓴다. 원래 Project의 데이터를 지우지 않는다. 녹화용 폴더의 `sample-materials/`에 합성 예제 4개가 있다. 공식 YorkU 자료라고 설명하지 않는다. API 설정 파일은 로컬에 준비하지만 화면에서 열지 않는다.
3. 1920x1080 또는 글자가 읽히는 해상도로 창을 최대화한다. 녹화 10초를 시험하고 재생해서 글자, 마이크, 알림음을 확인한다. 영상 도구가 없는 경우 기존 Windows 녹화 도구 또는 설치된 녹화 프로그램을 사용한다. 이 가이드는 별도 유료 프로그램 구매를 요구하지 않는다.
4. 각 장면 앞에 예를 들어 `F02 Syllabus Deadline Extraction`이라는 영어 제목을 넣는다. 아래 인용문을 나레이션 또는 자막으로 쓴다. 마우스를 빠르게 움직이지 말고 결과가 뜨면 2~3초 둔다.
5. GUI와 CLI를 동시에 실행하지 않는다. GUI를 닫고 같은 녹화 폴더의 CLI를 열면 저장된 일정과 공부시간을 그대로 확인할 수 있다.
6. AI 대기 구간을 잘라도 된다. 잘랐다는 표시를 넣고, 실제 실행 결과를 미리 정한 응답으로 바꾸지는 않는다. 생성되는 문제, 점수, 선택 번호는 매번 다를 수 있다.
7. API 키, .env 내용, API 관리 화면, 개인 메일, 브라우저 알림은 영상에 넣지 않는다.

### Windows에서 녹화 시작하는 방법

시작 메뉴에서 **Snipping Tool / 캡처 도구**를 검색해 연다. 위쪽 **Record / 녹화**를 선택하고 **New / 새로 만들기**를 누른다. 앱과 팝업이 모두 들어오도록 화면 영역을 드래그한 뒤 **Start / 시작**을 누른다. 끝나면 **Stop / 중지**를 누르고 MP4로 저장한다. 설치 버전에 마이크·시스템 오디오 옵션이 보이면 필요한 것을 켠 뒤 10초 시험 녹화를 재생한다. 음성을 녹음할 수 없는 버전이면 저장한 영상에 아래 영어 설명을 자막으로 넣는다. 녹화 시작 전에 알림과 개인 창을 닫는다.

영역 선택과 Start/Stop 순서는 [Microsoft의 Windows 녹화 안내](https://www.microsoft.com/en-us/windows/learning-center/how-to-record-screen-windows-11)를 따른다. 안내 페이지와 설치 버전의 오디오 기능은 다를 수 있으므로 소리 유무는 시험 재생으로 확인한다. 내보낸 영상은 `01-workspace-deadlines.mp4`, `02-study-agent.mp4`, `03-cli.mp4`로 구분하면 된다.

## 영상 1 Workspace and deadlines

권장 9~12분. 다음 장면 순서대로 녹화한다.

### V1.01 F01 N04 학기와 과목 만들기

New semester를 누른다. 이름은 `Fall 2026 Demo`, 시작은 `20260901`, 끝은 `20261231`로 입력한다. 날짜 옆 달력도 한 번 열어 날짜를 고를 수 있음을 보여준다. New course에서 `EECS3311`을 만들고 난이도 HIGH를 고른다. `EECS2030`을 LOW로 하나 더 만든다. 색상은 샘플을 클릭한다. 과목 우클릭의 Change course color도 보여준다. 범위를 벗어난 잘못된 날짜를 한 번 입력한 후 수정해 오류 처리를 보여준다.

> F01 manages semesters, courses and uploaded materials. N04 makes setup easier with compact dates, a calendar picker and color choices. Each course has its own difficulty.

### V1.02 F01 N01 여러 파일 업로드와 분류

EECS3311을 선택하고 Add files를 누른다. 녹화 폴더의 sample-materials에서 `demo-syllabus.txt`, `week3-lecture.txt`, `week3-lecture.pdf`, `design-review-specification.txt`를 Ctrl 또는 Shift로 함께 고른다. Review file folders에서 파일별 목적지를 읽고 AI sort files를 누른다. Syllabus 1개, Lecture Materials 2개, Assignment Specifications 1개인지 확인하고 Upload files를 누른다. 이유와 목적지 수정이 가능함을 보여준다.

강의 파일 하나를 우클릭 Move to another folder로 Assignment Specifications에 잠시 넣는다. AI 메뉴의 Classify uploaded files에서 강의로 옮기자는 이유를 읽고 승인한다. 원래 PDF가 그대로 열리는지 보여준다. 파일 하나를 다시 업로드해 중복 교체 여부를 묻는 흐름에서 취소 또는 건너뛰기를 보여준다.

> Files are stored locally. AI sorting is optional and must be reviewed before upload or moving. Uncertain classifications receive a full review of the extracted document, rather than relying only on its first page.

### V1.03 N02 F01 PDF와 강의 주차

week3-lecture.pdf를 클릭한다. 오른쪽 원본 PDF 탭에서 다음 페이지, 이전 페이지, Fit width를 누른다. 분할선을 움직이고 탭의 x로 닫는다. 모두 닫았을 때 오른쪽 창이 사라지는 것을 보여준다. 강의 파일을 선택해 AI > Suggest lecture week를 누르고 Week 3 제안을 검토해 승인한다. 필요하면 파일 우클릭에서 주차를 수동 지정하는 대안도 보여준다.

> N02 renders the original PDF locally in a closable side tab. Text extraction is separate and feeds search and AI. The model proposes a lecture week using the syllabus; I approve it before the week is stored.

### V1.04 F02 N03 일정 추출과 Add all

Analyze files for calendar를 누른다. 제안 제목, 날짜, 비중, 출처, evidence, warning을 보여준다. 예제의 Quiz 1은 November 20, Design Review는 November 21, Final Exam은 TBD다. 출처 TXT도 열어 대조한다. Review selected로 항목 하나를 열고 Edit 또는 Cancel을 보여준다. Add all을 누르면 Final Exam 날짜를 즉시 묻는다. Keep TBD를 선택한다. 같은 Design Review가 syllabus와 specification에 있어도 중복 일정이 생기지 않는지 확인한다. 승인 후 Calendar에 실제 항목이 생기는 것을 보여준다.

> F02 extracts assessment candidates and Java validates them. N03 reviews the batch, skips duplicates and asks about unknown dates immediately. No candidate changes the calendar before approval.

### V1.05 F03 F04 F09 달력과 우선순위와 진행률

Calendar에서 월 이동, Today, All courses와 한 과목 필터를 보여준다. 빈 날짜를 클릭해 일정을 추가한다. 같은 날짜에 EECS3311의 `High priority demo`와 EECS2030의 `Low priority demo`를 만든다. 날짜는 촬영 당일 또는 다음 날이며 반드시 학기 안으로 고른다. Timeline에서 날짜가 먼저이고 같은 날은 HIGH 과목이 먼저임을 보여준다. 헤더를 클릭해 정렬을 바꿀 수 있지만, 우선순위 설명 때는 기본 일정 순서를 보여준다. 일정 더블클릭으로 수정 창을 열고, 완료 표시 후 Course 페이지의 진행률 변화를 보여준다.

> F03 provides a month calendar and timeline. F04 orders earlier deadlines first and uses course difficulty for same-date ties. F09 tracks completed and overdue deadlines. The student still chooses the actual workload.

### V1.06 N06 드래그 선택과 우클릭

Timeline에서 두 행을 드래그해 선택한다. 선택한 행에서 우클릭해 `2 deadlines selected`를 보여준다. Mark completed를 누르고 두 행의 Done이 Yes가 되는지 보여준다. 다시 선택해 Mark incomplete로 되돌린다. Copy selected rows도 보여준다. 단일 행 우클릭에서는 Open details와 Edit deadline가 추가로 나온다. Delete 선택 후 확인창의 Cancel을 눌러 일정이 남아 있음을 보여준다. Ctrl/Shift 선택, Shift+F10 메뉴도 한 번 보여준다.

> N06 adds row selection and contextual actions. Selected identifiers are captured after sorting, so a refresh cannot redirect the action to a different deadline. Deletion requires confirmation. Dragging selects rows; it does not move deadline dates.

### V1.07 F07 N03 알림과 미정 날짜

오늘 날짜인 데모 일정 하나를 만든다. Schedule 메뉴의 notification 설정에서 현재보다 약 2분 뒤의 시각을 `HH:MM`으로 넣고 lead time은 `0`으로 한다. 앱을 켠 채 기다려 실제 알림과 제목을 보여준다. 기다린 구간은 편집 표시 후 줄여도 된다. Final Exam이 TBD인 사실과 하루 한 번 미정 날짜를 다시 묻는 기능을 설명한다. 같은 날 중복 팝업이 반복되지 않는다는 동작은 영상에서 설명하고, 다음 날 재등장은 자동 테스트로 확인한 범위임을 구분한다.

> F07 checks configured deadline times while the app is open, and missed notices appear on the next launch. Unknown assessment dates remain visible and receive a daily reminder. This is not an operating-system background service.

## 영상 2 Study and agent workflows

권장 10~14분. 영상 1에서 만든 같은 데이터를 이어 쓴다.

### V2.01 F05 공부 타이머

Study timer에서 15 min을 누른다. Timeline으로 갔다가 하단 remaining-time 문구를 클릭해 타이머로 돌아온다. Pause, Resume, Stop을 순서대로 쓰고 Study records에서 일별 합과 주별 합을 보여준다. 30 min, 1 hour 선택지도 보여준다. 완전 종료는 별도로 실제 15분을 기다려 촬영한 뒤 `15 minutes later` 표시로 짧게 편집해 넣는다. 완료 시 저장된 900초와 알림을 보여준다.

> F05 counts active study time, supports pause and resume, and saves sessions. Records combine sessions into daily and weekly totals. The timer is not linked to a particular course.

### V2.02 F06 F11 참고 파일과 출처 질문

Quiz 1 일정을 고른 뒤 Reference files 또는 AI > Suggest reference files를 누른다. week3 강의가 추천된 결과를 보여준다. EECS3311을 선택하고 Chat에서 아래 질문을 입력한 뒤 Ask sources를 누른다.

`What is the Open Closed principle? Explain it using the uploaded lecture.`

답의 파일명과 페이지 번호를 읽고 원본 PDF에서 맞는 부분을 연다. 이어서 자료에 없는 질문 `What is the instructor's private phone number?`를 물어보고 자료 부족을 알리는 대안을 보여준다. 응답이 근거 없는 내용을 만들면 그 결과를 성공으로 설명하지 말고 재검토한다.

> F06 recommends uploaded files for an assessment. F11 retrieves source pages, checks returned citations and adjusts explanation depth to the course. Recent history is context, but is not treated as source evidence.

### V2.03 F12 공부 노트

week3-lecture.pdf를 고르고 AI > Create study notes를 누른다. 제목, 핵심 bullet, 설명과 출처를 보여준다. Open source로 원본을 나란히 연다. 닫은 후 Saved study notes로 저장된 노트를 다시 연다.

> F12 builds and saves a study guide from uploaded material. The original PDF remains separately viewable. Generated explanations still need source review.

### V2.04 F13 객관식 퀴즈

AI > Generate quiz에서 One file, week3-lecture.pdf, 문제 수 `3`, difficulty MEDIUM, Multiple choice를 고른다. 다른 범위 Lecture week와 All lectures가 있음을 먼저 보여줘도 된다. 1번 답을 고르고 3번으로 이동한 후 1번으로 돌아와 답이 남아 있음을 보여준다. 2번을 비운 상태에서는 최종 Submit이 비활성화됨을 보여준다. 전부 답하고 제출한다. 정답/오답, 점수, 문제별 explanation을 보여준다. 일부 오답이 있으면 다음 약점 분석에 도움이 되지만 실제 정답을 미리 정했다고 설명하지 않는다.

> The model generates the questions and answer keys. The app grades multiple-choice answers locally, preserves selections during navigation and prevents incomplete submission. The review explains chosen and correct answers.

### V2.05 F13 에세이와 점수 승인

Generate quiz에서 문제 수 `2`, Essay를 선택해 두 문제가 생성되는 것을 보여준다. 답변에는 다음처럼 자료에 근거한 문장을 넣되 실제 질문에 맞게 수정한다.

`Command encapsulates a request as an object. The invoker holds it while the user reviews a proposal. Approval executes the command on the data store. Observer listeners then refresh the GUI after the store changes.`

AI 피드백과 제안 점수를 보여준다. Proposed score 입력에서 예를 들어 `85`로 수정하고 승인한다. 승인 전에는 점수가 확정 저장되지 않음을 설명한다. 취소한 문제는 나중에 Answer essay question에서 다시 열 수 있다. CLI의 essay list로 승인된 결과도 보여줄 것이다.

> Essay grading is an AI proposal. I can correct the score before approving it, and only approval saves the grade. Question difficulty is separate from the explanation style chosen for the course.

### V2.06 F14 약점과 자기평가

Study > Self assess에서 topic `SOLID`, confidence `2`를 넣는다. Weak topics에서 객관식 정답률과 자기평가를 보여준다. Review advice에서 업로드 강의 추천을 보여준다. 에세이 점수는 현재 객관식 topic accuracy에 합산되지 않는다고 설명한다.

> F14 combines multiple-choice topic accuracy and self-confidence, then asks the model for review advice using uploaded lectures. Essay marks are stored separately from this accuracy measure.

### V2.07 F08 자연어 일정 변경

EECS3311에서 AI > Request schedule change 또는 Chat의 Change schedule에 입력한다.

`Move Design Review to November 22, 2026. Keep its title, type and scope.`

before/after를 읽고 한 번 Reject한 뒤 같은 요청을 다시 보내 Approve한다. 승인 전에는 원래 날짜, 승인 후에는 11월 22일인 것을 Calendar에서 보여준다.

> F08 turns a natural-language request into a reviewed command. Java checks the target and date. Rejecting changes nothing; approval executes the stored command. One request handles one deadline action.

### V2.08 F10 F15 주간 브리핑과 기록

Weekly brief를 열어 일정, 저장된 공부시간, 약점이 함께 쓰이는 것을 보여준다. History에서 질문, 퀴즈 결과, 승인/거절 이벤트를 연다. 앱을 닫았다 다시 열어 데이터가 유지됨을 보여준다. 데모 기록 하나를 선택해 삭제 확인창을 취소하거나, 녹화 전용 데이터임을 확인하고 삭제한다.

> F10 combines schedule, study records and weak topics into a weekly briefing. F15 keeps selected interactions across restarts and supplies recent context to later questions. Pending unapproved proposals are not persisted.

## 영상 3 CLI and final reset

권장 7~10분. GUI를 먼저 닫는다. 같은 녹화 폴더의 Study Agent CLI.exe를 더블클릭한다. `study>` 오른쪽에 명령을 입력한다. 아래의 대괄호 설명은 입력할 글자가 아니다. 목록 번호는 화면에서 **이름을 보고** 고른다. 데이터가 달라지면 번호도 바뀌므로 숫자를 외워서 입력하지 않는다. 공란은 Enter 한 번이다.

### V3.01 F01 N04 실행과 안내

```text
help
tree
```

명령 목록과 GUI에서 만든 트리를 보여준다. 새 작업공간에서 처음 촬영한다면 다음을 쓴다. 이미 만든 같은 학기를 계속 쓸 때는 setup이 새 학기 질문을 건너뛰고 과목 추가부터 물을 수 있다.

```text
setup
Fall 2026 Demo
20260901
20261231
EECS3311
3
n
n
```

> This is the working CLI. It calls the same AgentController as the GUI and reads the same saved workspace. Guided setup accepts compact dates and assigns a course color automatically.

### V3.02 F01 F02 파일과 승인

기존 GUI 데이터로 촬영하면 먼저 `tree`로 파일을 보여준다. CLI 업로드 장면은 material add에서 과목 EECS3311, category `2` Lecture Materials를 고르고 파일 경로를 붙여넣는다. 이미 있으면 Replace 질문에 `n`을 넣고, 다음 File path는 공란으로 끝낸다. 새 강의 업로드이면 week 제안에 y 또는 n을 입력한다. 경로는 녹화 폴더의 sample-materials 파일 우클릭으로 경로 복사를 한 후 **둘러싼 따옴표 없이** 붙여넣는다.

```text
material add
[EECS3311 번호]
2
[week3-lecture.txt의 전체 경로]
n
[공란]
ai week
[week3-lecture.txt 번호]
proposals
approve
[Week 3 제안 번호]
ai syllabus
[demo-syllabus.txt 번호]
proposals
reject
[이미 추가한 일정의 중복 제안 번호]
```

추출을 승인하는 장면이 필요하면 아직 추가하지 않은 항목을 approve한다. GUI에서 추가한 일정을 무의미하게 중복 승인하지 않는다.

> The CLI supports material upload, lecture-week proposals and syllabus extraction. Approval and rejection are explicit commands; the model does not write the schedule directly.

### V3.03 F03 F04 F07 F09 일정 관리

```text
deadline add
[EECS3311 번호]
CLI practice
Assignment
20261123
SOLID
timeline
all
n
[공란]
calendar
2026-11
complete
[CLI practice 번호]
progress
[EECS3311 번호]
deadline notify
[CLI practice 번호]
12:00
60
reminders
30
notifications
```

timeline의 all 대신 week/month도 가능하다. Filter by course에 y를 넣으면 과목 번호를 묻는다. Search에는 `CLI practice` 같은 제목을 넣을 수 있다. 알림이 아직 해당 시각이 아니면 아무 것도 안 나오는 것이 정상이다. 실제 팝업 도달은 V1.07에서 보여준다.

> The CLI creates deadlines, filters and orders the timeline, displays a month calendar and tracks completion. Notification settings are persisted; a notice appears when its configured time is reached.

### V3.04 F06 F11 F12 공부 지원

```text
ai references
[Quiz 1 번호]
ask
[EECS3311 번호]
What is the Open Closed principle?
summary create
[week3-lecture.pdf 번호]
summary list
[같은 파일 번호]
```

출처 파일과 페이지를 보여준다. 생성에 잠시 걸리면 결과가 나온 뒤 화면을 읽을 시간을 둔다.

> These commands demonstrate real reference selection, grounded questions and saved study notes through the same AI workflows as the GUI.

### V3.05 F13 F14 퀴즈와 에세이

```text
quiz create
file
[week3-lecture.pdf 번호]
2
2
mcq
quiz take
[EECS3311 번호]
[방금 만든 퀴즈 번호]
[1번 문제 답 1~4]
[2번 문제 답 1~4]
self assess
[EECS3311 번호]
SOLID
2
weak topics
[EECS3311 번호]
weak advice
[EECS3311 번호]
essay list
[GUI에서 승인한 에세이의 원본 자료 번호]
```

quiz create의 file 대신 week 또는 all을 쓸 수 있다. week는 과목과 주차를 묻고, all은 과목을 묻는다. 타입은 mcq/essay/both다. Both는 2개 이상이어야 한다. CLI에서 에세이 전체도 보여주려면 아래를 덧붙인다.

```text
essay create
[week3-lecture.pdf 번호]
essay take
[EECS3311 번호]
[새 에세이 번호]
[질문에 맞춘 한 줄 답변]
proposals
approve
[essay grade 제안 번호]
essay list
[동일 자료 번호]
```

> Multiple-choice grading is deterministic. Essay feedback comes from the model and is saved only after approval. Weak-topic analysis uses quiz accuracy and self-confidence.

### V3.06 F05 F08 F10 F15 공유 저장과 계획과 기록

```text
study week
20261114
ai schedule
[EECS3311 번호]
Move CLI practice to November 24, 2026. Keep its title and type.
proposals
approve
[해당 변경 제안 번호]
weekly brief
[EECS3311 번호]
history
exit
```

study week는 타이머를 실행하는 명령이 아니라 그 주의 저장 기록을 읽는 명령이다. 실제 타이머를 다른 주에 촬영했다면 그 날짜를 넣는다. GUI에서 기록한 시간이 CLI에도 나타남을 보여준다.

> The CLI reads the study records saved by the GUI. Schedule requests remain proposals until approval. Briefing and persistent history are available in both interfaces.

### V3.07 N05 마지막 초기화

다른 시연과 CLI를 모두 끝낸 후 녹화 폴더의 GUI를 다시 연다. 빨간 Delete all local data를 눌러 숫자와 삭제 범위를 읽고 먼저 Cancel을 보여준다. 완전 초기화도 찍으려면 **녹화 폴더**임을 확인한 뒤 직접 승인하고 빈 트리를 보여준다. 원래 Project 앱에서 하지 않는다. 설정 파일이나 외부 원본 자료를 화면에 열지 않는다.

> N05 resets this local demonstration workspace after confirmation. It preserves the local API configuration and the external source files. This final reset is performed only on demonstration data.

## CLI 나머지 명령 참고

위 촬영 흐름에 꼭 모두 넣을 필요는 없지만 다음도 구현되어 있다. 명령 뒤 표시되는 질문 순서에 맞춰 입력한다.

| 명령 | 입력 순서 |
| --- | --- |
| semester add | 이름, 시작 날짜, 끝 날짜 |
| course add | 학기 번호가 나오면 선택, 과목명, 난이도 1/2/3 |
| deadline edit | 일정 번호, 새 제목, 새 타입, 새 날짜 또는 공란 TBD, 새 scope |
| deadline delete | 일정 번호, 삭제 확인 y/n |
| essay grade | 자료 번호, 질문, 한 줄 답변, 이후 approve/reject로 점수 제안 처리 |
| history delete | 기록 번호, 삭제 확인 y/n |
| quit | exit와 동일하게 종료 |

단일 학기나 과목만 있으면 그 선택 질문은 생략될 수 있다. setup, material add, ai week, notifications는 상태에 따라 추가 질문이나 출력이 생긴다. 고정된 입력을 한꺼번에 붙여넣기보다 화면의 프롬프트를 보면서 한 줄씩 입력한다.

## 촬영 후 제출 완료 순서

1. 세 영상을 재생해서 기능별 입력과 결과가 실제로 보이고 글자가 읽히는지 확인한다. 음성 또는 자막으로 기능 설명이 있는지 확인한다.
2. 교수/TA가 접근 요청 없이 볼 수 있는 호스트에 올린다. 접근 가능한 링크를 사용하고 계정에서 로그아웃한 상태나 시크릿 창에서 재생을 확인한다.
3. `docs/demo/demonstration-index.json`의 각 기능에 실제 URL과 시작 시간 `MM:SS`를 적는다. 계획 구간 번호는 실제 시간으로 둔갑시키지 않는다.
4. 그 링크와 시간을 stage2-report의 기능 mapping과 Demonstration index에 반영하고 PDF를 다시 만든다. 현재 보고서는 영상 미녹화 상태를 명시한 초안이다.
5. 보고서의 AI 비용 스냅샷과 알려진 제한을 확인한다. 개발 subscription과 앱 API 비용을 합쳐서 쓰지 않는다.
6. 승인받은 코드, 테스트, 보고서, 업데이트 다이어그램, 영상 색인을 같은 공개 GitHub 저장소에 커밋·푸시한다. API 키, 사용자 데이터, EXE, ZIP은 소스 커밋에 넣지 않는다.
7. GitHub의 최종 파일과 영상 링크를 확인한 뒤 LMS에 `https://github.com/jj990405/EECS3311`을 제출한다. 연장된 마감은 2026년 11월 15일 오후 11시 59분이며, LMS의 제출 완료 상태까지 확인한다.

이것까지 해야 제출이 끝난다. 주간 푸시 기간 요건은 별도이며 늦은 새 커밋으로 과거 푸시 시점을 만들 수 없다. Stage 3의 KUMA 검증을 이번 Stage 2 제출 영상과 혼동하지 않는다.
