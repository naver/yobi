이 문서는 윈도우를 재부팅했을 때 자동으로 Yobi를 실행하는 방법에 관한 것입니다.  

1. Win+R을 눌러 실행 창을 띄운 후 `shell:startup`을 입력하고 엔터를 누릅니다.
1. 빈 공간을 마우스 오른쪽 클릭 한 후 `새로 만들기`->`텍스트 문서`->`새 텍스트 문서.txt`를 지우고 `yobi.bat`로 수정한 후 엔터를 누릅니다.  (`yobi.bat` 파일을 생성해주세요)
1. `yobi.bat`를 텍스트 에디터로 연 다음 아래 내용을 입력해주세요.
```
cd "[적당한_yobi의_위치]\yobi"
del "RUNNING_PID"
..\play.bat start -DapplyEvolutions.default=true -Dhttp.port=이용할_포트
```

`[적당한_yobi의_위치]`의 예는 다음과 같습니다: `C:\Users\Newheart\Desktop\play-2.1.0\yobi`  
`[이용할_포트]`의 예는 다음과 같습니다: `9000`

수정을 마치고 저장하시면 재부팅 할 때마다 알아서 실행해줍니다.  

- `play.bat`를 실행할 때 붙이는 옵션은 기존에 사용하시던 대로 쓰셔도 됩니다.
