# 자바스크립트 모듈 작성 가이드

본 Markdown 문서는 http://markdownlivepreview.com/ 에서 정확한 모습을 확인할 수 있음

----
## 명명 규칙

* NamingGuide.md 참조

## 폴더 구조
* __javascripts/lib__

    jQuery를 포함한 프레임워크, 라이브러리 파일  

* __javascripts/common__

    공통 코드, UI Components.
    함수 또는 객체로 작성한다

* __javascripts/service__

    각 페이지에 사용되는 자바스크립트 모듈.
    함수로 작성한다

* __javascripts/deprecated__

    사용되지 않는 파일이지만 개발 과정에서 참조하기 위해 임시로 보존하는 코드

## 코드 구조
* hive.CodeTemplate.js 참조

>    * 초기화 함수는 \_init() 으로 통일하고, 이 함수 내에서 \_initVar, \_initElement, \_attachEvent 를 호출한다. 
>        각 함수의 순서는 필요에 따라 배치할 수 있고 굳이 해당 함수가 필요치 않는 경우 작성하지 않아도 무방하다.
>    * 모듈 내에서 사용되는 변수는 htVar의 멤버 변수로 할당하며 초기화 단계에서 수행하는 함수명은 \_initVar() 으로 한다
>    * 모듈 내에서 사용되는 HTML Element 변수는 htElement 의 멤버 변수로 할당하며 초기화 단계에서 수행하는 함수명은 \_initElement() 로 한다
>    * 특별히 제약이 없는 한 이벤트 핸들러 지정은 \_attachEvent 에서 일괄적으로 수행하도록 구성한다

* UI 컴포넌트 등 공통 코드는 함수, 객체 등 명명 규칙을 준수한다는 전제하에 코드 구조는 자유롭게 구성해도 무방하다 


## $hive.loadModule 사용법
* 만약 hive.module.Name 이라는 이름의 모듈을 정의한 경우 아래와 같다
* 첫번째 인자: hive. 을 제외한 모듈 이름
* 두번째 인자: 해당 모듈 초기화 함수에 제공될 옵션 객체(Object)

>       $hive.loadModule("module.Name", {
>            "sOption": "Option Value"
>        });

* 현재 페이지 내에서 해당 모듈을 찾을 수 없는 경우, 즉 명시적으로 <script> 태그를 통해 포함하지 않은 경우
    자동으로 javascripts/service 에서 동적으로 자바스크립트 파일을 로딩하려 시도한다.
    이미 페이지 내에 <script> 태그를 이용해 포함한 경우에는 동적 로딩은 시도되지 않는다.
    동적 로딩을 시도하는 파일 경로는 javascripts/service/hive.(module.Name).js 이다.
* 자바스크립트 파일 로딩이 완료되어 모듈 코드를 사용할 수 있을 때 자동으로 초기화를 시도하며 내부적으로 수행되는 코드는 아래와 같다.
    이 중 htOption 변수는 $hive.loadModule() 의 두번째 인자와 동일하다

>        new hive.module.Name(htOption)

* $hive.loadModule()는 모듈 함수를 실행하는 역할만 한다. 별도의 인터페이스가 필요한 것은 아니기 때문에
    모듈 내부의 함수 구조가 모듈 로딩 자체에 영향을 주지는 않는다.

## 기타

* HTML 템플릿은 가능한 한 자바스크립트 파일 내에 포함하지 않는다
* HTML 템플릿 데이터는 정적 페이지내에 <script type="text/template"> 형태로 위치시키고 자바스크립트는 그 내용을 활용하는 형태로 작성한다

>    <script type="text/template" id="tplItem">
>        ${name} ${email}
>    </script>
>
>    var sTpl = document.getElementById("tplItem").text;

