# Stage 2 전체 촬영 대본 - 한국어 조작 / 영어 나레이션

(작성일 2026년 11월 14일. 아래 구간 번호는 촬영 계획이며 실제 녹화 타임스탬프가 아니다.)


## 먼저 답

(교수에게 보여줄 것은 실제 프로그램의 입력, 실행, 결과다.)
(얼굴 촬영이나 특정 목소리 형식은 요구되지 않는다.)
(기능 ID와 이름, 무엇을 하는 기능인지, AI가 관여하는 지점을 설명해야 한다.)
(짧은 영어 나레이션을 읽는 방식을 권장한다.)
(음성을 쓰기 어렵다면 아래 영어 문장을 자막으로 넣고 읽을 시간을 준다.)
(둘 다 할 필요는 없다.)


(지침은 영상 개수나 분량을 정하지 않는다.)
(여기서는 세 영상으로 나눈다.)
(영상 1 약 9~12분, 영상 2 약 12~16분, 영상 3 약 8~12분, 총 29~40분을 계획한다. 실제 AI 대기와 타이머 대기는 표시를 넣어 줄일 수 있다. 이 분량은 권장이며 과제 제한 시간이 아니다.)
(아래의 괄호는 읽을 대사가 아니라 실제 조작이다.)
(영어 인용문만 소리 내어 읽는다.)
(원본 PDF를 보여주기만 하거나 슬라이드로 설명하는 것은 실행 시연을 대체하지 못한다.)
(각 기능은 반드시 결과까지 보여준다.)


## 준비와 실행

(로컬 `codex/recording-app-20261114/`의 **Study Agent.exe**를 녹화용으로 사용한다. **Study Agent CLI.exe**, app/, runtime/, sample-materials/를 함께 둔다. 다른 컴퓨터에서는 README대로 패키징한 실행 폴더를 사용한다. EXE 하나만 복사하지 않는다.)
(녹화용 트리가 비어 있는지 먼저 확인한다. 기존 데이터가 있다면 삭제 전에 녹화용 복사본인지 확인한다. 원래 Project 데이터는 지우지 않는다. GUI와 CLI는 동시에 켜지 않는다. 같은 녹화 폴더를 쓰면 CLI가 GUI의 저장 기록을 읽는다.)
(sample-materials/의 네 합성 예제를 사용한다. 공식 YorkU 자료라고 설명하지 않는다. 실제 과목 자료를 사용하면 질문과 날짜를 해당 자료에 맞춘다. 로컬 API 설정은 미리 준비하고 화면에 열지 않는다.)
(앱·퀴즈 창 글자가 읽히는 해상도로 최대화한다. 설치된 녹화 도구로 화면과 마이크를 10초 시험 촬영하고 재생한다. 개인 알림을 끈다. API 키·.env·인증·메일 화면은 넣지 않는다.)
(각 장면은 괄호의 클릭·입력 순서대로 실행하고 영어 인용문을 읽는다. 음성 대신 같은 영어를 자막으로 넣어도 된다. 입력과 실제 결과를 2~3초 남긴다. AI 대기를 편집할 때는 대기 생략 표시를 넣는다. 문항·점수·선택 번호는 매번 달라질 수 있으니 실제 화면을 본다.)
(영상 파일 이름 예: 01-workspace-deadlines.mp4, 02-study-agent.mp4, 03-cli.mp4. 각 장면 ID를 메모하고 녹화가 끝난 뒤 실제 시작 시간을 기록한다.)

## 영상 1 Workspace and deadlines

(권장 9~12분. 다음 장면 순서대로 녹화한다.)
(앱을 열기 직전에 아래 소개를 읽는다.)

> This is the Semester Study and Deadline Management Agent. I will demonstrate the running Java GUI and CLI, including source-based AI study tools and schedule changes that require student approval. The materials in this recording are synthetic examples.

### V1.01 F01 N04 학기와 과목 만들기

(녹화용 폴더의 Study Agent.exe를 더블클릭하고 창을 최대화한다.)
(상단 New semester를 누른다.)
(이름은 `Fall 2026 Demo`로 입력한다. Start date에서 `20260230`을 넣어 잘못된 날짜 안내를 보여준 뒤 `20260901`로 고친다. End date는 `20261231`로 입력한다.)
(날짜 옆 달력도 한 번 열어 날짜를 고를 수 있음을 보여준다.)
(New course에서 `EECS3311`을 만들고 Blue 색상과 Hard 난이도를 고른다.)
(`EECS2030`을 Violet 색상과 Easy 난이도로 하나 더 만든다.)
(GUI의 Hard/Easy가 내부 HIGH/LOW에 대응한다.)
(색상은 샘플을 클릭한다.)
(과목 우클릭의 Change course color도 보여준다.)


> F01 manages semesters, courses and uploaded materials. N04 makes setup easier with compact dates, a calendar picker and color choices. Each course has its own difficulty.

### V1.02 F01 N01 여러 파일 업로드와 분류

(EECS3311을 선택하고 Add files를 누른다.)
(녹화 폴더의 sample-materials에서 `demo-syllabus.txt`, `week3-lecture.txt`, `week3-lecture.pdf`, `design-review-specification.txt`를 Ctrl 또는 Shift로 함께 고른다.)
(Review file folders에서 파일별 목적지를 읽고 AI sort files를 누른다.)
(Syllabus 1개, Lecture Materials 2개, Assignment Specifications 1개인지 확인하고 Upload files를 누른다.)
(이유와 목적지 수정이 가능함을 보여준다. 업로드 후 전체 페이지 읽기·이미지 판독·검색 인덱스 저장이 끝나 Ready for questions and quizzes가 표시될 때까지 기다린다. 이후 퀴즈는 저장된 자료를 재사용한다.)


(강의 파일 하나를 우클릭 Move to another folder로 Assignment Specifications에 잠시 넣는다.)
(상단 AI actions의 Classify uploaded files에서 강의로 옮기자는 이유를 읽고 승인한다.)
(원래 PDF가 그대로 열리는지 보여준다.)
(파일 하나를 다시 업로드해 중복 교체 여부를 묻는 흐름에서 취소 또는 건너뛰기를 보여준다.)
(이제 업로드된 파일을 왼쪽 트리, 오른쪽 Files 목록, Course 페이지에서 각각 선택해 하이라이트를 보여준다. 파일을 우클릭해 AI: Study notes, AI: Quiz this file, AI: Quiz pages / slides가 보이는지 보여준다. 목록 파일은 더블클릭하거나 Enter로 연다.)


> F01 stores original files locally. N01 provides reviewed file classification and moves. AI sorting is optional and must be reviewed before upload or moving. Upload reads all pages and stores complete text with a local page and word index. Image-heavy slides receive model-assisted reading through the configured API. Later study tools reuse the cache. Sorting still requires review, and generated readings need source checking.

### V1.03 N02 F01 PDF와 강의 주차

(week3-lecture.pdf를 클릭한다.)
(오른쪽 원본 PDF 탭에서 다음 페이지, 이전 페이지, Fit width를 누른다.)
(분할선을 움직이고 탭의 x로 닫는다.)
(모두 닫았을 때 오른쪽 창이 사라지는 것을 보여준다.)
(강의 파일을 선택해 AI actions > Suggest lecture week를 누르고 Week 3 제안을 검토해 승인한다.)
(필요하면 파일 우클릭에서 주차를 수동 지정하는 대안도 보여준다.)


> N02 renders the original PDF locally in a closable side tab. Text extraction is separate and feeds search and AI. The model proposes a lecture week using the syllabus; I approve it before the week is stored.

### V1.04 F02 N03 일정 추출과 Add all

(상단 AI actions > Analyze files for calendar를 누른다.)
(제안 제목, 날짜, 비중, 출처, evidence, warning을 보여준다.)
(예제의 Quiz 1은 November 20, Design Review는 November 21, Final Exam은 TBD다.)
(출처 TXT도 열어 대조한다.)
(Review selected로 항목 하나를 열고 Edit 또는 Cancel을 보여준다.)
(Add all을 누르면 Final Exam 날짜를 즉시 묻는다.)
(Keep TBD를 선택한다.)
(같은 Design Review가 syllabus와 specification에 있어도 중복 일정이 생기지 않는지 확인한다.)
(승인 후 Calendar에 실제 항목이 생기는 것을 보여준다.)


> F02 extracts assessment candidates and Java validates them. N03 reviews the batch, skips duplicates and asks about unknown dates immediately. No candidate changes the calendar before approval.

### V1.05 F03 F04 F09 달력과 우선순위와 진행률

(Calendar에서 월 이동, Today, All courses와 한 과목 필터를 보여준다. 일정 추가 직전에는 필터를 All courses로 돌리고 November 2026을 연다.)
(빈 11월 16일을 클릭하고 Add deadline을 누른다. EECS3311을 고르고 제목 High priority demo, 타입 Assignment, scope SOLID로 저장한다. 같은 날을 다시 클릭해 EECS2030의 Low priority demo, 타입 Quiz, scope Data structures를 추가한다.)
(두 일정 모두 2026년 11월 16일을 사용해 같은 날짜의 난이도 우선순위를 비교한다.)
(Timeline에서 날짜가 먼저이고 같은 날은 HIGH 과목이 먼저임을 보여준다.)
(헤더를 클릭해 정렬을 바꿀 수 있지만, 우선순위 설명 때는 기본 일정 순서를 보여준다.)
(일정을 더블클릭해 상세 창을 연 뒤 Edit로 수정한다. Toggle completed로 완료 처리하고 Course 페이지의 진행률 변화를 보여준다.)


> F03 provides a month calendar and timeline. F04 orders earlier deadlines first and uses course difficulty for same-date ties. F09 tracks completed and overdue deadlines. The student still chooses the actual workload.

### V1.06 N06 드래그 선택과 우클릭

(Timeline에서 두 행을 드래그해 선택한다.)
(선택한 행에서 우클릭해 `2 deadlines selected`를 보여준다.)
(Mark completed를 누르고 두 행의 Done이 Yes가 되는지 보여준다.)
(다시 선택해 Mark incomplete로 되돌린다.)
(Copy selected rows도 보여준다.)
(단일 행 우클릭에서는 Open details와 Edit deadline가 추가로 나온다.)
(Delete 선택 후 확인창의 Cancel을 눌러 일정이 남아 있음을 보여준다.)
(Ctrl/Shift 선택, Shift+F10 메뉴도 한 번 보여준다.)


> N06 adds row selection and contextual actions. Selected identifiers are captured after sorting, so a refresh cannot redirect the action to a different deadline. Deletion requires confirmation. Dragging selects rows; it does not move deadline dates.

### V1.07 F07 N03 알림과 미정 날짜

(오늘 날짜인 데모 일정 하나를 만든다.)
(Schedule 메뉴의 notification 설정에서 현재보다 약 2분 뒤의 시각을 `HH:MM`으로 넣고 lead time은 `0`으로 한다.)
(앱을 켠 채 기다려 실제 알림과 제목을 보여준다.)
(기다린 구간은 편집 표시 후 줄여도 된다.)
(Final Exam이 TBD인 사실과 하루 한 번 미정 날짜를 다시 묻는 기능을 설명한다.)
(같은 날 중복 팝업이 반복되지 않는다는 동작은 영상에서 설명하고, 다음 날 재등장은 자동 테스트로 확인한 범위임을 구분한다.)


> F07 checks configured deadline times while the app is open, and missed notices appear on the next launch. Unknown assessment dates remain visible and receive a daily reminder. This is not an operating-system background service.

## 영상 2 Study and agent workflows

(권장 12~16분. 영상 1에서 만든 같은 데이터를 이어 쓴다.)


### V2.01 F05 공부 타이머

(Study timer에서 15 min을 누른다.)
(Timeline으로 갔다가 하단 remaining-time 문구를 클릭해 타이머로 돌아온다.)
(Pause, Resume, Stop을 순서대로 쓰고 Study records에서 일별 합과 주별 합을 보여준다.)
(30 min, 1 hour 선택지도 보여준다.)
(완전 종료는 별도로 실제 15분을 기다려 촬영한 뒤 `15 minutes later` 표시로 짧게 편집해 넣는다.)
(완료된 15분 세션이 900초로 저장된 결과와 알림을 보여준다. 앞서 멈춘 짧은 세션이 있으면 하루 총합은 900초보다 크다.)


> F05 counts active study time, supports pause and resume, and saves sessions. Records combine sessions into daily and weekly totals. The timer is not linked to a particular course.

### V2.02 F06 F11 참고 파일과 출처 질문

(EECS3311의 week3-lecture.pdf를 선택하고 Chat 입력란에 hi를 입력한 뒤 Enter를 누른다.)
(안내 응답과 선택한 파일의 범위 표시를 보여준다.)
(인사는 로컬 안내이며 공부 질문부터 실제 AI API를 사용한다고 설명한다.)
(Chat에서 quiz를 입력하면 퀴즈 설정과 실제 풀이 화면이 열린다. notes는 공부 노트, slide quiz는 페이지 범위 퀴즈로 연결된다. V2.04에서 quiz 입력 경로를 사용해도 된다.)


(Quiz 1 일정을 고른 뒤 Reference files 또는 AI actions > Suggest reference files를 누른다.)
(week3 강의가 추천된 결과를 보여준다.)
(week3-lecture.pdf를 다시 선택하고 Chat의 범위 드롭다운을 Selected file only로 바꾼 뒤 아래 질문을 입력하고 Ask sources를 누른다. Course materials가 기본값이며 이 경우 선택 파일이 있어도 과목 전체에서 찾는다.)


`What is the Open Closed principle? Explain it using the uploaded lecture.`

(답의 파란색 밑줄 출처에서 파일명·페이지를 클릭한다. 오른쪽 PDF 탭이 같은 파일의 정확한 물리 페이지로 이동하는지 보여준다. Chat 안의 출처도 같은 방식으로 동작한다.)
(답변이 Chat 안에 한 번만 나타나고 중복 팝업이 없는지 보여준다. 긴 답은 Chat 안에서 스크롤한다. 일반 AI 답변은 Chat에만 표시한다. 설정·확인·오류 창은 남아 있고, 퀴즈 풀이가 별도 작업 창을 사용한다.)
(Course materials 범위에서 `How should I prepare for Quiz 1?`을 묻는다. 실제 자료로 녹화한다면 `How should I prepare for Lab Test 2?`를 사용한다. syllabus의 날짜 안내만이 아니라 관련 강의의 개념, 실제 예제 연습, 주의점, 자가 점검 질문이 나오고 강의 페이지도 인용되는지 보여준다.)
(자료가 없는 EECS2030을 선택해 같은 공부 질문을 보내 입력 부족 안내를 보여준다.)
(다시 EECS3311의 강의 PDF로 돌아온다.)
(응답이 근거 없는 내용을 만들거나 오류가 나면 그 결과를 성공으로 설명하지 말고 재검토한다.)


> F06 recommends uploaded files for an assessment. F11 validates source citations and adjusts explanation depth to the course. For preparation questions, the syllabus identifies scope, then the app selects relevant lectures and reads their complete content. Large scopes use page-validated source notes. The answer gives concepts, practice tasks and self-checks, rather than just dates. Course-wide search is the default; selected-file scope is explicit. Earlier student questions supply conversational context. These are source-based recommendations, not guaranteed exam questions.

### V2.03 F12 공부 노트

(week3-lecture.pdf를 고르고 우클릭 AI: Study notes 또는 상단 AI actions > Create study notes를 누른다.)
(상단 AI working 경과 시간과 Cancel AI, 하단 읽기 진행 상황을 보여준다.)
(취소 경로도 보이려면 한 번 Cancel AI 후 재실행한다.)
(완료된 노트의 제목, 핵심 bullet, 설명과 출처를 보여준다.)
(Open source PDF로 원본을 나란히 연다.)
(닫은 후 Saved study notes로 저장된 노트를 다시 연다.)


> F12 builds and saves a study guide from uploaded material. The app reads bounded, page-labelled source batches, shows progress and allows cancellation. The original PDF remains separately viewable, and saved notes can be reopened. Generated explanations still need source review.

### V2.04 F13 객관식 퀴즈

(AI actions > Practice quiz에서 One file, week3-lecture.pdf, 문제 수 `3`, difficulty MEDIUM, Multiple choice를 고른다.)
(파일 범위에는 Lecture Materials만 사용하며 syllabus로 퀴즈를 만들지 않는다. 다른 범위 Lecture week와 All lectures가 있음을 먼저 보여줘도 된다. 주차가 하나도 지정되지 않은 상태에서 Lecture week를 선택하면 강의 파일 선택 표가 바로 열린다. 사용할 파일을 체크하면 주차를 억지로 지정하지 않고 진행한다. 전체 강의는 모든 저장 페이지를 분할해서 읽는다.)
(1번 답을 고르고 3번으로 이동한 후 1번으로 돌아와 답이 남아 있음을 보여준다.)
(2번을 비운 상태에서는 최종 Submit이 비활성화됨을 보여준다.)
(전부 답하고 제출한다.)
(정답/오답, 점수, 문제별 explanation을 보여준다.)
(일부 오답이 있으면 다음 약점 분석에 도움이 되지만 실제 정답을 미리 정했다고 설명하지 않는다.)


> F13 is quiz generation and grading. The model generates the questions and answer keys. The app grades multiple-choice answers locally, preserves selections during navigation and prevents incomplete submission. The review explains chosen and correct answers.

(원본 week3-lecture.pdf의 2쪽으로 이동한다.)
(PDF 상단 AI 메뉴 또는 PDF 본문 우클릭의 Quiz this slide를 누른다.)
(기본 페이지가 2인지 확인하고 문항 수 1, 난이도 MEDIUM으로 생성한다.)
(실제 답을 선택하고 Submit quiz를 누른 뒤 점수와 해설까지 보여준다.)


(파일 우클릭 AI: Quiz pages / slides에서 0을 넣어 범위 오류를 보여준다.)
(다시 열어 1-2를 넣으면 범위 지정이 가능함을 보여준다.)
(표지나 목차만 고르면 내용 부족 안내가 나올 수 있으므로 개념이 있는 페이지로 바꾼다.)
(범위 퀴즈는 객관식이며, 선택 페이지 밖의 내용은 입력하지 않는다.)


> F13 also supports selected-slide quizzes. I can choose the current PDF page or a page range. Java validates the range and sends only those selected pages to the model. A cover or outline may not contain enough study concepts; in that case I choose concept slides or a wider range. The resulting multiple-choice score is calculated locally.

### V2.05 F13 에세이와 점수 승인

(상단 AI actions > Practice quiz에서 One file과 강의 PDF를 고른 뒤 문제 수 `2`, 난이도 MEDIUM, Question types에서 Essay를 선택해 두 문제가 생성되는 것을 보여준다. 파일 선택 창이 생략되면 현재 선택한 PDF가 사용된다.)
(답변에는 다음처럼 자료에 근거한 문장을 넣되 실제 질문에 맞게 수정한다.)


`Command encapsulates a request as an object. The invoker holds it while the user reviews a proposal. Approval executes the command on the data store. Observer listeners then refresh the GUI after the store changes.`

(AI 피드백과 제안 점수를 보여준다.)
(Proposed score 입력에서 예를 들어 `85`로 수정하고 승인한다.)
(실제 질문이 예시와 다르면 질문에 맞게 답변도 바꾼다.)
(승인 전에는 점수가 확정 저장되지 않음을 설명한다.)
(취소한 문제는 나중에 Answer essay question에서 다시 열 수 있다.)
(CLI의 essay list로 승인된 결과도 보여줄 것이다.)


> F13 essay grading is an AI proposal. I can correct the score before approving it, and only approval saves the grade. Question difficulty is separate from the explanation style chosen for the course.

### V2.06 F14 약점과 자기평가

(Study > Self assessment에서 topic `SOLID`, confidence `2`를 넣는다.)
(Weak topics에서 객관식 정답률과 자기평가를 보여준다.)
(상단 AI actions > Weak topics / review advice에서 업로드 강의 추천을 보여준다.)
(에세이 점수는 현재 객관식 topic accuracy에 합산되지 않는다고 설명한다.)


> F14 combines multiple-choice topic accuracy and self-confidence, then asks the model for review advice using uploaded lectures. Essay marks are stored separately from this accuracy measure.

### V2.07 F08 자연어 일정 변경

(EECS3311에서 AI actions > Request schedule change 또는 Chat의 Change schedule에 입력한다.)


`Move Design Review to November 22, 2026. Keep its title, type and scope.`

(before/after를 읽고 한 번 Reject한 뒤 같은 요청을 다시 보내 Approve한다.)
(승인 전에는 원래 날짜, 승인 후에는 11월 22일인 것을 Calendar에서 보여준다.)


> F08 turns a natural-language request into a reviewed command. Java checks the target and date. Rejecting changes nothing; approval executes the stored command. One request handles one deadline action.

### V2.08 F10 F15 주간 브리핑과 기록

(Weekly brief를 열어 일정, 저장된 공부시간, 약점이 함께 쓰이는 것을 보여준다.)
(History에서 질문, 퀴즈 결과, 승인/거절 이벤트를 연다.)
(앱을 닫았다 다시 열어 데이터가 유지됨을 보여준다.)
(데모 기록 하나를 선택해 삭제 확인창을 취소하거나, 녹화 전용 데이터임을 확인하고 삭제한다.)


> F10 combines schedule, study records and weak topics into a weekly briefing. F15 keeps selected interactions across restarts and supplies recent context to later questions. Pending unapproved proposals are not persisted.

## 영상 3 CLI and final reset

(권장 8~12분. GUI를 먼저 닫는다.)
(같은 녹화 폴더의 Study Agent CLI.exe를 더블클릭한다.)
(`study>` 오른쪽에 명령을 입력한다.)
(아래의 대괄호 설명은 입력할 글자가 아니다.)
(목록 번호는 화면에서 **이름을 보고** 고른다.)
(데이터가 달라지면 번호도 바뀌므로 숫자를 외워서 입력하지 않는다.)
(공란은 Enter 한 번이다.)


### V3.01 F01 N04 실행과 안내

```text
help
tree
material index
```

(명령 목록과 GUI에서 만든 트리를 보여준다. material index는 추가 입력 없이 전체 저장 자료를 읽거나 캐시를 재사용한다. 완료 결과를 보여준다.)
(이미 GUI에서 EECS3311을 만들었다면 아래 setup은 실행하지 않는다. 중복 과목이 생길 수 있다. 별도 빈 작업공간에서 wizard를 추가로 촬영할 때만 다음을 쓴다.)
(이미 만든 같은 학기를 계속 쓸 때는 setup이 새 학기 질문을 건너뛰고 과목 추가부터 물을 수 있다.)


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

> This is the working CLI for F01 workspace management and N04 guided setup. It calls the same AgentController as the GUI and reads the same saved workspace. Guided setup accepts compact dates and assigns a course color automatically.

### V3.02 F01 F02 파일과 승인

(기존 GUI 데이터로 촬영하면 먼저 `tree`로 파일을 보여준다.)
(CLI 업로드 장면은 material add에서 과목 EECS3311, category `2` Lecture Materials를 고르고 파일 경로를 붙여넣는다.)
(이미 있으면 Replace 질문에 `n`을 넣고, 다음 File path는 공란으로 끝낸다.)
(새 강의 업로드이면 week 제안에 y 또는 n을 입력한다.)
(경로는 녹화 폴더의 sample-materials 파일 우클릭으로 경로 복사를 한 후 **둘러싼 따옴표 없이** 붙여넣는다.)


| 명령 / 실제 질문 | 입력 또는 조작 |
| --- | --- |
| material add | EECS3311 번호 → Category 2 Lecture Materials → week3-lecture.txt 전체 경로 |
| 기존 파일 Replace? | n → 다음 File path는 공란으로 종료 |
| 신규 파일 업로드인 경우 | 주차 제안 y/n에 답한 후 다음 File path는 공란으로 종료 |
| ai week | week3-lecture.txt 번호 |
| proposals → approve | Week 3 제안 번호 |
| ai syllabus | demo-syllabus.txt 번호 |
| proposals → reject | 이미 추가한 일정의 중복 제안 번호 |


(추출을 승인하는 장면이 필요하면 아직 추가하지 않은 항목을 approve한다.)
(GUI에서 추가한 일정을 무의미하게 중복 승인하지 않는다.)


> F01 supports material upload and lecture-week proposals in the CLI. F02 provides syllabus deadline extraction. Approval and rejection are explicit commands; the model does not write the schedule directly.

### V3.03 F03 F04 F07 F09 일정 관리

| 명령 | 실제 질문 순서대로 입력 |
| --- | --- |
| deadline add | EECS3311 번호 → CLI practice → Assignment → 20261123 → SOLID |
| timeline | all → n → 공란 검색 |
| calendar | 2026-11 |
| complete | CLI practice 번호 |
| progress | EECS3311 번호 |
| deadline notify | CLI practice 번호 → 12:00 → 60 |
| reminders | 30 |
| notifications | 추가 입력 없음 |

(일정 날짜는 compact date가 가능하지만 calendar의 Month 입력은 YYYY-MM 형식이다. timeline의 all 대신 week/month도 가능하다.)
(Filter by course에 y를 넣으면 과목 번호를 묻는다.)
(Search에는 `CLI practice` 같은 제목을 넣을 수 있다.)
(알림이 아직 해당 시각이 아니면 아무 것도 안 나오는 것이 정상이다.)
(실제 팝업 도달은 V1.07에서 보여준다.)


> F03 provides CLI deadline and calendar management. F04 orders the timeline, and F09 tracks completion. F07 provides saved notification settings and due notices. Notification settings are persisted; a notice appears when its configured time is reached.

### V3.04 F06 F11 F12 공부 지원

| 명령 | 실제 질문 순서대로 입력 |
| --- | --- |
| ai references | Quiz 1 번호 |
| ask | EECS3311 번호 → What is the Open Closed principle? |
| ask | EECS3311 번호 → How should I prepare for Quiz 1 using the uploaded lectures? |
| summary create | week3-lecture.pdf 번호 |
| summary list | 같은 파일 번호 |

(출처 파일과 페이지를 보여준다.)
(생성에 잠시 걸리면 결과가 나온 뒤 화면을 읽을 시간을 둔다.)


> These commands demonstrate F06 reference selection, F11 grounded questions and F12 saved study notes through the same AI workflows as the GUI.

### V3.05 F13 F14 퀴즈와 에세이

| 명령 | 실제 질문 순서대로 입력 |
| --- | --- |
| quiz create | file → week3-lecture.pdf 번호 → 문제 수 2 → 난이도 2 → mcq |
| quiz take | EECS3311 번호 → 방금 만든 퀴즈 번호 → 실제 1번·2번 답 각 1~4 |
| self assess | EECS3311 번호 → SOLID → 2 |
| weak topics | EECS3311 번호 |
| weak advice | EECS3311 번호 |
| essay list | GUI에서 승인한 에세이의 원본 파일 번호 |

(quiz create의 file 대신 pages, week 또는 all을 쓸 수 있다.)
(pages는 파일 번호 다음에 1-2 같은 페이지 범위를 물으며 mcq 타입을 사용한다. 표의 화살표는 한꺼번에 입력할 문자가 아니라 각 프롬프트 순서다.)
(슬라이드 범위도 촬영하려면 quiz create → pages → PDF 번호 → 1-2 → 2 → 2 → mcq 순으로 한 번 더 실행하고 풀이·제출 결과까지 보여준다.)
(week는 과목과 주차를 묻고, all은 과목을 묻는다.)
(타입은 mcq/essay/both다.)
(Both는 2개 이상이어야 한다.)
(CLI에서 에세이 전체도 보여주려면 아래를 덧붙인다.)


| 명령 | 실제 질문 순서대로 입력 |
| --- | --- |
| essay create | week3-lecture.pdf 번호 |
| essay take | EECS3311 번호 → 새 에세이 번호 → 질문에 맞는 한 줄 답변 |
| proposals → approve | 해당 essay grade 제안 번호 |
| essay list | 같은 원본 파일 번호 |

> F13 grades multiple-choice answers locally. Essay feedback comes from the model and is saved only after approval. F14 uses quiz accuracy and self-confidence to suggest review work.

### V3.06 F05 F08 F10 F15 공유 저장과 계획과 기록

| 명령 | 실제 질문 순서대로 입력 |
| --- | --- |
| study week | 타이머 촬영 날짜, 예: 20261114 |
| ai schedule | EECS3311 번호 → Move CLI practice to November 24, 2026. Keep its title and type. |
| proposals → approve | 해당 일정 변경 제안 번호 |
| calendar | 2026-11. 승인 후 11월 24일로 바뀐 결과 확인 |
| weekly brief | EECS3311 번호 |
| history | 추가 입력 없음 |
| exit | CLI 종료 |

(study week는 타이머를 실행하는 명령이 아니라 그 주의 저장 기록을 읽는 명령이다.)
(실제 타이머를 다른 주에 촬영했다면 그 날짜를 넣는다.)
(GUI에서 기록한 시간이 CLI에도 나타남을 보여준다.)


> F05 reads the study records saved by the GUI. F08 provides approved schedule changes, F10 provides weekly briefings, and F15 provides persistent history. Schedule requests remain proposals until approval. Briefing and persistent history are available in both interfaces.

### V3.07 N05 마지막 초기화

(다른 시연과 CLI를 모두 끝낸 후 녹화 폴더의 GUI를 다시 연다.)
(빨간 Delete all local data를 눌러 숫자와 삭제 범위를 읽고 먼저 Cancel을 보여준다.)
(완전 초기화도 찍으려면 **녹화 폴더**임을 확인한 뒤 직접 승인하고 빈 트리를 보여준다.)
(원래 Project 앱에서 하지 않는다.)
(설정 파일이나 외부 원본 자료를 화면에 열지 않는다.)


> N05 is confirmed local-data reset. It resets this demonstration workspace after confirmation. It preserves the local API configuration and the external source files. This final reset is performed only on demonstration data.

## CLI 나머지 명령 참고

(위 촬영 흐름에 꼭 모두 넣을 필요는 없지만 다음도 구현되어 있다.)
(명령 뒤 표시되는 질문 순서에 맞춰 입력한다.)


| 명령 | 입력 순서 |
| --- | --- |
| semester add | 이름, 시작 날짜, 끝 날짜 |
| course add | 학기 번호가 나오면 선택, 과목명, 난이도 1/2/3 |
| deadline edit | 일정 번호, 새 제목, 새 타입, 새 날짜 또는 공란 TBD, 새 scope |
| deadline delete | 일정 번호, 삭제 확인 y/n |
| essay grade | 자료 번호, 질문, 한 줄 답변, 이후 approve/reject로 점수 제안 처리 |
| history delete | 기록 번호, 삭제 확인 y/n |
| quit | exit와 동일하게 종료 |

(단일 학기나 과목만 있으면 그 선택 질문은 생략될 수 있다.)
(setup, material add, ai week, notifications는 상태에 따라 추가 질문이나 출력이 생긴다.)
(고정된 입력을 한꺼번에 붙여넣기보다 화면의 프롬프트를 보면서 한 줄씩 입력한다.)


## 촬영 후 제출 완료 순서

(세 영상을 재생해서 기능별 입력과 결과가 실제로 보이고 글자가 읽히는지 확인한다.)
(음성 또는 자막으로 기능 설명이 있는지 확인한다.)
(교수/TA가 접근 요청 없이 볼 수 있는 호스트에 올린다.)
(접근 가능한 링크를 사용하고 계정에서 로그아웃한 상태나 시크릿 창에서 재생을 확인한다.)
(`docs/demo/demonstration-index.json`의 각 기능에 실제 URL과 시작 시간 `MM:SS`를 적는다.)
(계획 구간 번호는 실제 시간으로 둔갑시키지 않는다.)
(그 링크와 시간을 stage2-report의 기능 mapping과 Demonstration index에 반영하고 PDF를 다시 만든다.)
(현재 보고서의 구현 설명은 최신 코드로 갱신되어 있다. 실제 영상이 없으므로 Section 2·7의 링크와 시작 시간만은 촬영 후 완성해야 한다.)
(보고서의 AI 비용 스냅샷과 알려진 제한을 확인한다.)
(개발 subscription과 앱 API 비용을 합쳐서 쓰지 않는다.)
(승인받은 코드, 테스트, 보고서, 업데이트 다이어그램, 영상 색인을 같은 공개 GitHub 저장소에 커밋·푸시한다.)
(API 키, 사용자 데이터, EXE, ZIP은 소스 커밋에 넣지 않는다.)
(GitHub의 최종 파일과 영상 링크를 확인한 뒤 LMS에 `https://github.com/jj990405/EECS3311`을 제출한다.)
(연장된 마감은 2026년 11월 15일 오후 11시 59분이며, LMS의 제출 완료 상태까지 확인한다.)


(이것까지 해야 제출이 끝난다.)
(주간 푸시 기간 요건은 별도이며 늦은 새 커밋으로 과거 푸시 시점을 만들 수 없다.)
(Stage 3의 KUMA 검증을 이번 Stage 2 제출 영상과 혼동하지 않는다.)

## 과제 영상 요건 대조

| 지침의 요구 | 이 대본에서 보여주는 방법 |
| --- | --- |
| 모든 구현 기능, 추가·변경 기능 포함 | F01-F15와 N01-N06을 아래 색인으로 모두 연결한다. 슬라이드 퀴즈는 확장된 F13에 포함한다. |
| 기능 ID와 이름 식별 | 장면 시작의 영어 대사에서 ID와 역할을 말하고, 보고서 구간 이름에도 ID를 쓴다. |
| 호출 방법과 입력 | 한국어 괄호의 클릭 경로·입력값·CLI 프롬프트를 실제로 실행한다. |
| 실제 실행·결과 | 파일, 달력 변경, 퀴즈 점수·해설, 노트와 저장 기록까지 화면에 남긴다. |
| 기능 설명과 AI 역할 | 영어 대사에서 AI 생성·검색·제안과 Java 검증·로컬 계산·승인을 구분한다. |
| 적절한 오류·대안 | 잘못된 날짜·페이지, 중복·오분류 수정, TBD, 빈 날짜, 미응답 제출 차단, 거절·취소를 보여준다. |
| GUI와 CLI 모두 | 영상 1-2는 실제 GUI, 영상 3은 실제 CLI다. |
| 기능별 직접 영상 링크·타임스탬프 | 촬영 후 실제 URL과 시작 시간을 보고서에 넣는다. 이 계획 번호는 실제 시간이 아니다. |
| 교수·TA 접근 | 업로드 후 접근 요청 없이 재생되는지 별도 창에서 확인한다. |
| 비밀 보호 | API 키·.env·인증 화면·개인 알림을 촬영하지 않는다. |

## 기능별 촬영 색인

| 기능 | 촬영 구간 |
| --- | --- |
| F01 Workspace/materials  | V1.01, V1.02, V1.03, V3.01, V3.02 |
| F02 Syllabus deadlines  | V1.04, V3.02 |
| F03 Calendar/timeline  | V1.05, V3.03 |
| F04 Priority  | V1.05, V3.03 |
| F05 Study timer/records  | V2.01, V3.06 |
| F06 Reference files  | V2.02, V3.04 |
| F07 Deadline notices  | V1.07, V3.03 |
| F08 Natural-language edits  | V2.07, V3.06 |
| F09 Progress  | V1.05, V3.03 |
| F10 Weekly brief  | V2.08, V3.06 |
| F11 Grounded Q&A  | V2.02, V3.04 |
| F12 Summaries/study notes  | V2.03, V3.04 |
| F13 Quiz/essay  | V2.04, V2.05, V3.05 |
| F14 Weak topics  | V2.06, V3.05 |
| F15 History/memory  | V2.08, V3.06 |
| N01 Reviewed file classification and moves | V1.02 |
| N02 On-demand source workspace | V1.03, V2.03 |
| N03 Bulk calendar import and undated reminders | V1.04, V1.07 |
| N04 Easier setup and date/color selection | V1.01, V3.01 |
| N05 Confirmed full reset | V3.07 |
| N06 Timeline selection and context actions | V1.06 |
