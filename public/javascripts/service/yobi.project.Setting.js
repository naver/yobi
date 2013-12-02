/**
 * @(#)yobi.project.Setting.js 2013.03.18
 *
 * Copyright NHN Corporation.
 * Released under the MIT license
 * 
 * http://yobi.dev.naver.com/license
 */

(function(ns){

    var oNS = $yobi.createNamespace(ns);
    oNS.container[oNS.name] = function(htOptions){

        var htVar = {};
        var htElement = {};

        /**
         * initialize
         */
        function _init(htOptions){
            var htOpt = htOptions || {};
            _initVar(htOpt);
            _initElement(htOpt);
            _attachEvent();

            htVar.waPopOvers.popover();
        }

        /**
         * initialize variables
         * 정규식 변수는 한번만 선언하는게 성능 향상에 도움이 됩니다
         */
        function _initVar(htOptions){
            htVar.rxLogoExt = /\.(gif|bmp|jpg|jpeg|png)$/i;
            htVar.rxPrjName = /^[0-9A-Za-z-_\.]+$/;
            htVar.aReservedWords = [".", "..", ".git"];
        }

        /**
         * initialize element variables
         */
        function _initElement(htOptions){
            // 프로젝트 설정 관련
            htElement.welForm = $("form#saveSetting");
            htElement.welInputLogo = $("#logoPath");
            htElement.welInputName = $("input#project-name");

            htElement.welBtnSave   = $("#save");

            // popovers
            htVar.waPopOvers = $([$("#project_name"), $("#share_option_explanation"), $("#terms")]);
        }

        /**
         * attach event handlers
         */
        function _attachEvent(){
            htElement.welInputLogo.change(_onChangeLogoPath);
            htElement.welBtnSave.click(_onClickBtnSave);
        }

        /**
         * 프로젝트 로고 변경시 이벤트 핸들러
         */
        function _onChangeLogoPath(){
            var welTarget = $(this);

            // 확장자 규칙 검사
            if(!htVar.rxLogoExt.test(welTarget.val())){
                $yobi.showAlert(Messages("project.logo.alert"));
                welTarget.val('');
                return;
            }

            htElement.welForm.submit();
        }

        /**
         * 프로젝트 설정 저장 버튼 클릭시
         */
        function _onClickBtnSave(){
            var sPrjName = htElement.welInputName.val();
            if(!htVar.rxPrjName.test(sPrjName)){
                $yobi.showAlert(Messages("project.name.alert"));
                return false;
            }
            if(htVar.aReservedWords.indexOf(sPrjName) >= 0){
                $yobi.showAlert(Messages("project.name.reserved.alert"));
                return false;
            }

            return true;
        }

        _init(htOptions);
    };

})("yobi.project.Setting");
