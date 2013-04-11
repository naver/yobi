/**
 * @(#)hive.project.Member.js 2013.03.18
 *
 * Copyright NHN Corporation.
 * Released under the MIT license
 * 
 * http://hive.dev.naver.com/license
 */

(function(ns){
	
	var oNS = $hive.createNamespace(ns);
	oNS.container[oNS.name] = function(htOptions){

        var htVar = {};
		var htElement = {};
		
		/**
		 * initialize
		 */
		function _init(){
			_initElement();
			_attachEvent();
		}
		
		/**
		 * initialize element variables
		 */
		function _initElement(){
			htElement.waBtns = $(".btns");
			
			// 멤버 삭제 확인 대화창
			htElement.welAlertDelete = $("#alertDeletion");
			htElement.welBtnConfirmDelete = htElement.welAlertDelete.find(".btnDelete");
			
			// 멤버별 권한 선택
			htElement.waAccess = $(".dropdown-menu li");

            $('#loginId').typeahead().data('typeahead').source = _userTypeaheadSource
		}
		
		/**
		 * attach event handlers
		 */
		function _attachEvent(){
			htElement.waBtns.click(_onClickBtns);
			htElement.waAccess.click(_onClickAccess);
		}

		/**
		 * 각 멤버별 권한 선택시 처리 핸들러
		 */
		function _onClickAccess(){
			var welTarget = $(this);
			var welContainer = welTarget.closest(".btn-group");
			welContainer.find("button.d-label").text(welTarget.text());
			welContainer.find("li").removeClass("active");
			welTarget.addClass("active");			
		}
		
		/**
		 * 각 멤버별 버튼 클릭시 이벤트 핸들러
		 * data-action 속성을 바탕으로 분기
		 */
		function _onClickBtns(weEvt){
			var welTarget = $(weEvt.target);
			if(!welTarget.attr("data-action")){ // in case of <i class="ico">
				welTarget = $(welTarget.parent("[data-action]"));
			}
			
			var sAction = welTarget.attr("data-action").toLowerCase();;

			switch(sAction){
				case "delete":
					_onClickDelete(welTarget);
					break;
				case "apply":
					_onClickApply(welTarget);
					break;
			}
			return;
		}
		
		/**
		 * 멤버 삭제 버튼 클릭시
		 */
		function _onClickDelete(welTarget){
			var sURL = welTarget.attr("data-href");
			_showConfirmDeleteMember(sURL);
		}
		
		function _showConfirmDeleteMember(sURL){
			htElement.welBtnConfirmDelete.attr("href", sURL);
			htElement.welAlertDelete.modal();
		}
		
		/**
		 * 멤버 정보 변경 버튼 클릭시
		 */
		function _onClickApply(welTarget){
			var sURL = welTarget.attr("data-href");
			var sLoginId = welTarget.attr("data-loginId");
			var sUserId  = $("#roles-" + sLoginId).find(".active > a[data-id]").attr("data-id");
			
			if(typeof sUserId == "undefined"){
				console.log("cannot find user id");
				return false;
			}
			
			// send request
			$hive.sendForm({
				"sURL"   : sURL,
				"htData" : {"id": sUserId},
				"fOnLoad": function(){
					document.location.reload();
				}
			});
		}

        /**
        * Return whether the given content range is an entire range for items.
        * e.g) "items 10/10"
        *
        * @param {String} contentRange the vaule of Content-Range header from response
        * @return {Boolean}
        */
        function _isEntireRange(contentRange) {
            var result, items, total;

            if (contentRange) {
                result = /items\s+([0-9]+)\/([0-9]+)/.exec(contentRange);
                if (result) {
                    items = parseInt(result[1]);
                    total = parseInt(result[2]);
                    if (items < total) {
                        return false;
                    }
                }
            }

            return true;
        }

        /**
        * Data source for loginId typeahead while adding new member.
        *
        * For more information, See "source" option at
        * http://twitter.github.io/bootstrap/javascript.html#typeahead
        *
        * @param {String} query
        * @param {Function} process
        */
        function _userTypeaheadSource(query, process) {
            if (query.match(htVar.lastQuery) && htVar.isLastRangeEntire) {
                process(htVar.cachedUsers);
            } else {
                $('<form action="/users" method="GET">')
                    .append($('<input type="hidden" name="query">').val(query))
                    .ajaxForm({
                        "dataType": "json",
                        "success": function(data, status, xhr) {
                            htVar.isLastRangeEntire = _isEntireRange(xhr.getResponseHeader('Content-Range'));
                            htVar.lastQuery = query;
                            htVar.cachedUsers = data;
                            process(data);
                        }
                    }).submit();
            }
        }
		
		_init(htOptions);
	};
	
})("hive.project.Member");