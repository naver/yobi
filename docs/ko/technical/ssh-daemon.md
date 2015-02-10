이 문서는 SSH 서버 데몬의 내부 동작 및 사용 방법을 설명한다.


SSH서버 데몬
--------------
Yobi의 SSH서버 데몬은 Git서비스를 SSH프로토콜로 이용할 수 있게 한다.
이를 위해 다음과 같은 기능을 지원한다.
- 사용자가 SSH키를 통해 인증할 수 있도록 한다.
 - SSH키는 공개키-개인키로 이루어진 키쌍으로 인증 시 passphrase를 입력한다.
- 인증 된 사용자가 git-upload-pack, git-receive-pack명령을 기반으로 동작하는 clone, push, pull 등을 사용할 수 있도록 한다.
 - Git저장소에 대한 권한관리는 HTTP방식과 동일하다. (인증을 성공해도 해당 저장소에 권한이 없으면 거절된다)
 - 사용자가 git-upload-pack, git-receive-pack명령 이외의 명령을 서버에 전달할 수 없도록 한다.
- 사용자가 SSH를 통해 서버의 쉘에 직접 접근 할 수 없도록 한다.


데몬의 초기화 과정
--------------------
SshDaemon은 서버 실행 후 Global객체가 생성 될 때 Global객체 내부의 멤버 인스턴스로 생성된다.
Global객체의 onStart메서드가 실행 될 때, 이미 생성 되어 있는 SshDaemon인스턴스의 start메서드를 호출하여 데몬을 초기화하고 실행한다.
SSH가 사용하는 포트는 conf/application.conf의 ssh.port 값을 사용하되, 값이 정의되어있지 않다면 22000번을 사용한다.
데몬이 시작되면 사용자는 SSH프로토콜을 통해 Git저장소에 접근이 가능해 진다.
만약 데몬 시작에 실패했다면 Yobi의 프로젝트 홈 화면에서 SSH주소 복사 기능이 노출되지 않는다.


SSH서버의 SSH키 생성
----------------------
SshDaemon이 실행된 후 사용자가 최초로 SSH로 접근을 시도하면 `PEMGeneratorHostKeyProvider`를 통해 서버의 호스트 키(Host Key)가 생성된다.
`PEMGeneratorHostKeyProvider`는 MINA-SSHD에 구현되어 있는 기능으로 SSH 호스트 키(Host Key)를 생성한다.
이 객체의 생성자에는 호스트 키(Host Key)의 알고리즘, 키 사이즈 및 저장 할 디렉터리를 지정 할 수 있다.
생성된 호스트 키(Host Key)는 Yobi서버의 홈 디렉터리, 정확히는 자바 시스템 프로퍼티 `user.dir`가 가리키는 경로에 `yobihostkey.pem`파일로 저장된다.
PEM은 키 파일을 인코딩하는 방법으로, PEM 파일은 PEM Header, base64형식으로 인코딩 된 키 데이터, PEM Footer로 이루어져 있다.
Yobi의 호스트 키(Host Key)는 RSA알고리즘을 사용하며 저장되는 키 파일의 형식은 다음과 같다.
```
-----BEGIN RSA PRIVATE KEY-----
MIIEowIBAAKCAQEAhTYBJi8JCu9qgVltGHErfrwQfQ2c7XvgrwZ6Fn23MpkSC8ZT
[...]
mlv58VeP6w83HM95Ux7ODZkhMlD+kXx+ZfQEzX+FCqgSVO/i5YLO
-----END RSA PRIVATE KEY-----
```
호스트 키(Host Key)가 한번 생성되면 다시 생성하지 않고 동일한 키를 사용하게 된다.
만약 서버의 키 파일이 변경됐다면 과거에 해당 서버에 접속했던 사용자는 다음과 같은 경고 메시지와 함께 접속을 거절당한다.
```
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
@    WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!     @
@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!
Someone could be eavesdropping on you right now (man-in-the-middle attack)!
It is also possible that a host key has just been changed.
The fingerprint for the RSA key sent by the remote host is
[...]
Please contact your system administrator.
Add correct host key in ~/.ssh/known_hosts to get rid of this message.
Offending RSA key in ~/.ssh/known_hosts:1
RSA host key for localhost has changed and you have requested strict checking.
Host key verification failed.
```
이 경우 사용자는 ~/.ssh/known_hosts파일을 수정해야 접속이 가능하다.
- known_hosts파일은 사용자가 SSH로 접속을 시도했던 서버들의 공개키(Public Key) 정보를 담고 있다.

**known_hosts파일 수정 방법**
```
$ ssh-keygen -R "hostname or ip address"

위 명령은 known_hosts에서 hostname이나 ip address에 해당하는 공개키 정보를 삭제한다.
이 후 다시 SSH로 접속을 시도하면 다음과 같은 메시지와 함께 known_hosts에 새로운 키를 등록한다.

The authenticity of host 'localhost (127.0.0.1)' can't be established.
RSA key fingerprint is [...].
Are you sure you want to continue connecting (yes/no)? yes
Warning: Permanently added 'localhost' (RSA) to the list of known hosts.
```


SSH주소 형식
--------------
Yobi에서 SSH를 통해 Git저장소에 접근할 때는 다음과 같이 ssh://로 시작하는 url을 사용한다.
```
ssh://username@server:port/owner/project
```
- **username** - SSH접근에 사용하는 계정명이다. 계정명은 "yobi"로 고정해 사용한다. 다른 계정명은 접근이 거절된다.
- **server** - SSH로 접근할 서버의 주소다. hostname(domain)이나 ip address를 사용한다.
- **:port** - SSH로 접근할 포트 번호다. 22번(well-known)포트를 사용할 경우 생략 가능하다.
- **owner** - 프로젝트(Git저장소)의 소유자명이다. 저장소의 소유자가 개인일 경우 개인의 계정명, 그룹일 경우 그룹명이된다.
- **project** - 프로젝트(Git저장소)의 이름이다.

따라서 항상 앞 부분은 `ssh://yobi@`로 시작하고 뒷 부분은 접근하려는 서버, 포트번호, 소유자, 프로젝트명에 따라 변할 수 있다.

**사용 예시**
- ex1) ssh://yobi@localhost:2200/group1/firstproject
- ex2) ssh://yobi@yobi.test.com/person1/myprivateproject
- ex3) ssh://yobi@127.0.0.1:22/person2/localproject


사용자의 인증 과정 [1]
------------------------
SSH사용자가 접근을 시도하면 데몬은 SshPublicKeyAuth를 통해 사용자가 제공한 공개키(Public Key)를 확인한다.
먼저, 사용자가 제공한 공개키와 일치하는 공개키가 데이터베이스에 존재하지 않을 경우는 다음과 같이 처리한다.
- 1. 서버는 클라이언트에게 실패했다는 정보 `SshConstants.SSH_MSG_USERAUTH_FAILURE (51)`를 담은 메시지를 전달한다.
- 2. 클라이언트는 이 메시지를 바탕으로 사용자의 터미널에 `Permission denied`를 출력하고 연결을 종료한다.

다음으로, 공개키가 일치하는 경우에는 다음과 같이 처리한다.
- 1. 서버는 클라이언트에게 공개키가 유효하다는 정보 `SshConstants.USERAUTH_PK_OK (60)`를 담은 메시지를 전달하고 응답을 기다린다.
- 2. 클라이언트는 공개키와 매치되는 개인키(Private Key)의 passphrase를 사용자에게 입력하도록 요구한다.
- 3-1. 만약, 사용자가 passphrase를 3회까지 틀릴 경우 더 이상 인증이 진행되지 않고 종료된다.
- 3-2. 반대로 passphrase를 정상적으로 입력했다면 클라이언트는 공개키-개인키 쌍이 성립한다는 정보(true)와, 암호화 알고리즘 방식을 서버에 전달한다.
- 4. 서버는 이를 바탕으로 응답확인 정보 `SshConstants.SSH_MSG_USERAUTH_REQUSET (50)`와 인증성공 정보 `SshConstants.SSH_MSG_USERAUTH_SUCCESS (52)`를 포함하는 메시지를 클라이언트에게 전달한다.


SSH Daemon의 요청 처리
------------------------
Yobi의 SSH데몬은 Git서비스를 위한 데몬이므로 Git저장소에 접근하는 목적 이외의 SSH요청은 제한되어야 한다.

SSH는 체널을 개설하고자 하는 측에서 Channel type을 명시하도록 되어 있다.[2]
그러므로 MINA-SSHD는 사용자의 요청이 도착하면 사용자의 요청에 명시된 Channel type을 확인한다.[3]

명시된 Channel type이 'shell'인 경우, 쉘 접속 요청으로 판단 SshShellFactory에서 처리된다.
Yobi의 SshShellFactory는 사용자가 쉘 접속을 시도 할 경우 쉘 사용이 불가능함을 안내하며 아래와 같이 출력된다.
```
$ ssh yobi@domain
Hi user1! You've successfully authenticated, but [Yobi] does not provide shell access.
```

명시된 Channel type이 'exec'인 경우 SshCommandFactory로 처리된다.
Git의 clone, push, pull 등의 명령은, 내부적으로 git-upload-pack이나 git-receive-pack명령으로 변환된다.
만약 SSH프로토콜을 통해 git clone명령이 실행되면 아래와 같이 동작한다.[4]
```
$git clone ssh://yobi@domain/owner/project
		       |
		       v
$ssh yobi@domain "git-upload-pack '/owner/project'"
```
Yobi의 SshCommandFactory는 git-upload-pack과 git-receive-pack명령을 처리하도록 작성되어 있다.
만약 이 두가지 이외의 명령이 전송된다면 UnknownCommand로 예외 처리한다.


사용자의 공개키 관리
----------------------
Yobi의 사용자는 설정 페이지의 SSH키 설정 메뉴를 통해 본인의 공개키를 관리 할 수 있다.
공개키는 사용자가 작성한 키 파일의 설명과 함께 등록이 가능하다.
등록된 키는 최초 등록일, 마지막 사용 날짜가 출력되며 만약 공개키가 도용되어 인증에 실패한 요청도 사용한 날짜로 간주하여 업데이트한다.
등록된 키는 사용자의 판단에 따라 삭제가 가능하다.


참고문헌
----------
[1] **rfc4252**(The Secure Shell (SSH) Authentication Protocol) - http://www.ietf.org/rfc/rfc4252.txt
[2] **rfc4254**(Opening a Channel) - http://tools.ietf.org/html/rfc4254#section-5
[3] https://github.com/apache/mina-sshd/blob/sshd-0.13.0/sshd-core/src/main/java/org/apache/sshd/server/channel/ChannelSession.java#L278
[4] **Packfile transfer protocols**(SSH Transport) - https://github.com/git/git/blob/master/Documentation/technical/pack-protocol.txt
