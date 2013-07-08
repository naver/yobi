/**
 * @(#)hive.Label.js 2013.03.13
 *
 * Copyright NHN Corporation.
 * Released under the MIT license
 * 
 * http://hive.dev.naver.com/license
 */

/**
 * hive.Label
 * 라벨 목록을 가져다 표현해주는 역할 
 * (개별 삭제 링크 처리 포함)
 */
hive.Label = (function(htOptions){

	var htVar = {};
	var htElement = {};
	
	/**
	 * 초기화 
	 * initialize
	 * @param {Hash Table} htOptions
	 * @param {Boolean} htOptions.bEditable     라벨 편집기 활성화 여부 (default: false)
	 * @param {String} htOptions.sTplLabel      라벨 카테고리 그룹 템플릿
	 * @param {String} htOptions.sTplControls   라벨 그룹 템플릿
	 * @param {String} htOptions.sTplBtnLabelId 라벨 항목 템플릿
	 * @param {String} htOptions.sURLLabels     라벨 목록을 반환하는 AJAX URL
	 * @param {String} htOptions.sURLPost       새 라벨 작성을 위한 AJAX URL
	 */
	function _init(htOptions){
		htOptions = htOptions || {"bEditable": false};
		
		_initVar(htOptions);
		_initElement(htOptions);

		// initialize label editor on bEditable == true
		if(htVar.bEditable){
			_initLabelEditor();
		}
		
		_attachEvent();
		_getLabels(htOptions.fOnLoad);		
	}
	
	/**
	 *  변수 초기화
	 *  initialize variable except element
	 *	htVar.sURLLabels = htOptions.sURLLabels;
	 *	htVar.sURLPost = htOptions.sURLPost;
	 *	htVar.bEditable = htOptions.bEditable;
	 *  @param {Hash Table} htOptions 초기화 옵션
	 */
	function _initVar(htOptions){
		// copy htOptions to htVar
		for(key in htOptions){
			htVar[key] = htOptions[key];
		}
		
		htVar.sURLLabel = htVar.sURLLabels.substr(0, htVar.sURLLabels.length-1);
		
		htVar.sTplLabel = htOptions.sTplLabel || '<div class="control-group"><label class="control-label" data-category="${category}">${category}</label></div>';
		htVar.sTplControls = htOptions.sTplControls || '<div class="controls label-group" data-category="${category}"></div>';		
		htVar.sTplBtnLabelId = htOptions.sTplBtnLabelId || '<button type="button" class="issue-label ${labelCSS}" data-labelId="${labelId}">${labelName}</button>';		
	}
	
	/**
	 * 엘리먼트 초기화
	 * initialize element variable
	 */
	function _initElement(){
		htElement.welContainer  = $("fieldset.labels").empty();
		htElement.welForm = $('form#issue-form,form.form-search,form#search');
		
		// add label
		htElement.welLabels = $('.labels'); 
		htElement.welLabelEditor = $('.label-editor'); 		
	}
	
	/**
	 * 이벤트 핸들러 설정
	 * initialize event handler
	 */
	function _attachEvent(){
		htElement.welForm.submit(_onSubmitForm);		
	}
	
	/**
	 * 폼 전송시 라벨 선택도 반영되도록 자동으로 필드 추가
	 */
	function _onSubmitForm(){
		var aValues = [];
		var welButtons = $('fieldset.labels div[data-category] button.active[data-labelId]');
		
		welButtons.each(function(nIndex, elBtn){
			aValues.push('<input type="hidden" name="labelIds" value="'+ $(elBtn).attr('data-labelId') + '">');
		});
		
		htElement.welForm.append(aValues);
		welButtons = aButtons = null;

		return true;
	}

	/**
	 * 라벨 편집기 초기화
	 * initialize Label Editor
	 */
	function _initLabelEditor(){
		hive.LabelEditor.appendTo(htElement.welContainer, {
			"sURLPost" : htVar.sURLPost,
			"fOnCreate": _onCreateNewLabel
		});
	}
	
	/**
	 * 라벨 에디터가 새 라벨 생성후 호출하는 함수
	 * @param {Object} oLabel
	 */
	function _onCreateNewLabel(oLabel){
		_addLabelIntoCategory(oLabel);
		_setActiveLabel(oLabel.id, oLabel.color);
	}
	
	/**
	 * getLabels
	 * 라벨 목록을 서버로부터 수신하여 목록 생성 
	 * 분류에 따라 라벨 목록을 만드는 것은 _addLabelIntoCategory 에서 수행
	 * @param {Function} fCallback 완료 후 수행할 콜백 함수
	 */
	function _getLabels(fCallback){
		var fOnLoad = function(oRes) {
			if(!(oRes instanceof Object)){
				console.log('Failed to update - Server error');
				return;
			}

			// add label into category after sort
			var aLabels = oRes.sort(function(a, b) {
				return (a.category == b.category) ? (a.name > b.name) : (a.category > b.category);
			});
			$(aLabels).each(function(nIndex, oLabel){
				_addLabelIntoCategory(oLabel);
			});

			// run callback function
			if (typeof fCallback == "function") {
				fCallback();
			}
			
			aLabels = null;
		};

		// send request
		$hive.sendForm({
			"sURL"     : htVar.sURLLabels,
			"fOnLoad"  : fOnLoad,
			"htOptForm": {"method":"get"},
		});
	}

	/**
	 * add label into category
	 * 새 라벨을 지정한 분류에 추가. 
	 * 서버 통신용 함수는 아니고 화면에 표시하기 위한 목적. 
	 * @param {Number} oLabel.id 라벨 id
	 * @param {String} oLabel.category 라벨 분류
	 * @param {String} oLabel.name 라벨 이름
	 * @param {String} oLabel.color 라벨 색상
	 * @return {Wrapped Element} 추가된 라벨 버튼 엘리먼트
	 */
	function _addLabelIntoCategory(oLabel) {
		// set Label Color
		_setLabelColor(oLabel);
		
		// label Id		
		var welBtnLabelId = $.tmpl(htVar.sTplBtnLabelId, {
			"labelId": oLabel.id,
			"labelName": oLabel.name,
			"labelCSS" : 'active-' + $hive.getContrastColor(oLabel.color)
		});
		
		// 편집모드: 라벨 버튼을 항상 active 상태로 유지하고, 삭제 링크를 추가
		if(htVar.bEditable){ 
            welBtnLabelId.addClass('active');
			welBtnLabelId.append(_getDeleteLink(oLabel.id, oLabel.color));
		} else {
            welBtnLabelId.click(_onClickLabel);
        }
		
		// 이미 같은 카테고리가 있으면 거기에 넣고
		var welCategory = $('fieldset.labels div[data-category="' + oLabel.category + '"]');
		if (welCategory.length > 0) {
			welCategory.append(welBtnLabelId);
			return welBtnLabelId;
		}
		
		// 없으면 새 카테고리 줄을 추가한다
		var welLabel = $.tmpl(htVar.sTplLabel, {"category": oLabel.category});
		var welControls = $.tmpl(htVar.sTplControls, {"category": oLabel.category});
		welControls.append(welBtnLabelId); // Edit Button
		welLabel.append(welControls); // Controls

		if(htVar.bEditable){
			hive.LabelEditor.addCategory(oLabel.category);
		}

		// add label into category
		if(htElement.welLabelEditor.length > 0) { 
			htElement.welLabelEditor.before(welLabel);
		} else {
			htElement.welLabels.prepend(welLabel);
		}

		return welBtnLabelId;
	}

	/**
	 * 라벨 엘리먼트가 활성화 되었을때 (= active 클래스가 주어졌을때)
	 * 적용할 배경색/글자색 CSS Rule 추가하는 함수
	 * @param {Object} oLabel 
	 */
	function _setLabelColor(oLabel){
		var sCSSTarget = '.labels .issue-label.active[data-labelId="' + oLabel.id + '"]';
		document.styleSheets[0].addRule(sCSSTarget, 'background-color: ' + oLabel.color);
		document.styleSheets[0].addRule(sCSSTarget, 'color: ' + $hive.getContrastColor(oLabel.color));
		
		sCSSTarget = '.labels .issue-label[data-labelId="' + oLabel.id + '"]';
		document.styleSheets[0].addRule(sCSSTarget, 'border-left: 3px solid ' + oLabel.color);
	}

	/**
	 * 라벨 엘리먼트를 클릭했을때 이벤트 핸들러
	 * active 클래스를 토글한다
	 * @param {Wrapped Event} weEvt
	 * @return {Boolean} false
	 */
	function _onClickLabel(weEvt){
        var welTarget = $(weEvt.target || weEvt.srcElement || weEvt.originalTarget);
        welTarget.toggleClass("active");
        return false;
	}
	
	/**
	 * 라벨 삭제
     * remove label
	 * @param {String} sLabelId
	 */
	function _removeLabel(sLabelId) {
		var welLabel = $('[data-labelId=' + sLabelId + ']');
		
		if (welLabel.siblings().size() > 0) {
			welLabel.remove();
			return;
		}
		
		var sCategory = $(welLabel.parents('div').get(0)).attr('data-category');
		$('[data-category="' + sCategory + '"]').parent().remove();
		
		// 라벨 에디터의 분류 자동완성에서도 제거
		if(htVar.bEditable){
			hive.LabelEditor.removeCategory(sCategory);
		}
	}

	/**
	 * 라벨 삭제 링크 반환
     * Get delete link element
	 * @param {String} sId
	 * @param {String} sColor
	 * @return {Wrapped Element}
	 */
	function _getDeleteLink(sId, sColor){
		var fOnClick = function(){
			if(confirm(Messages("label.confirm.delete")) === false){
				return false;
			}
			
			$hive.sendForm({
				"sURL"   : htVar.sURLLabel + '/' + sId + '/delete',
				"htData" : {"_method": "delete"},
				"fOnLoad": function(){
					_removeLabel(sId);					
				}
			});	
		};
		
		var welLinkDelete = $('<a class="delete">&times;</a>');
		    welLinkDelete.click(fOnClick);

		return welLinkDelete;
	}
	
	/**
	 * 지정한 라벨을 선택한 상태로 만들어주는 함수
	 * @param {String} sId
	 * @param {String} sColor deprecated
	 */
	function _setActiveLabel(sId, sColor){
		// 색상 지정: addLabelIntoCategory 단계에서 
		// 이미 .active 상태의 색상이 지정되어 있음

		// 해당되는 라벨 엘리먼트에 active 클래스 지정
	    $('.labels button.issue-label[data-labelId="' + sId + '"]').addClass('active');		
	}
	
	// 인터페이스 반환
	return {
		"init": _init,
		"setActiveLabel": _setActiveLabel
	};
})();

/**
 * hive.LabelEditor
 * 새 라벨 추가를 위한 에디터 인터페이스
 */
hive.LabelEditor = (function(welContainer, htOptions){
	
	var htVar = {};
	var htElement = {};
	
	/**
	 * 초기화
	 * initialize
	 * @param {Wrapped Element} welContainer Container element to append label editor
	 * @param {Hash Table} htOptions
	 */
	function _init(welContainer, htOptions){
		_initVar(htOptions);
		_initElement(welContainer);
		_attachEvent();
	}
	
	/**
	 * 변수 초기화
	 * initialize variables
	 * @param {Hash Table} htOptions
	 * @param {Function} htOptions.fOnCreate    라벨 추가 후 실행할 콜백 함수
     * @param {String}   htOptions.sURLPost     라벨 추가 AJAX URL
	 * @param {String}   htOptions.sTplEditor   라벨 에디터 템플릿
	 * @param {String}   htOptions.sTplBtnColor 라벨 기본 색상 버튼 템플릿
     * @param {Array}    htOptions.aColors      라벨 기본 색상코드 배열
	 */
	function _initVar(htOptions){
		htVar.sURLPost = htOptions.sURLPost;
		htVar.fOnCreate = htOptions.fOnCreate || function(){};

		htVar.aColors = htOptions.aColors || ['#999999','#da5454','#f86ca0','#ff9e9d','#ff9933','#ffcc33','#f8c86c','#99ca3c','#22b4b9','#4d68b1','#6ca6f8','#3fb8af','#9966cc','#ffffff'];
		htVar.sTplEditor = htOptions.sTplEditor || '<div class="control-group label-editor">\
		<strong class="control-label">${labelNew}</strong>\
		<div id="custom-label" class="controls">\
            <div class="colors"><input type="text" name="labelColor" class="input-small labelColor" placeholder="${labelCustomColor}"></div>\
            <div class="row-fluid">\
                <div class="span6">\
        			<input type="text" name="labelCategory" class="input-small labelInput" data-provider="typeahead" autocomplete="off" placeholder="${labelCategory}">\
                </div>\
                <div class="span6">\
        			<input type="text" name="labelName" class="input-small labelInput" placeholder="${labelName}" autocomplete="off">\
                </div>\
            </div>\
            <div class="row-fluid">\
                <div class="span12"><button type="button" class="nbtn medium black labelSubmit">${labelAdd}</button></div>\
            </div>\
		</div></div>';
		htVar.sTplBtnColor = htOptions.sTplBtnColor || '<button type="button" class="issue-label issueColor nbtn small" style="background-color:${color}">';		
	}
	
	/**
	 * 엘리먼트 초기화
	 * initialize elements
	 * @param {Wrapped Element} welContainer 컨테이너 엘리먼트
	 */
	function _initElement(welContainer){
	    // htVar.sTplEditor 를 이용해 만들어진 라벨 에디터를 대상 영역에 붙이고
		htElement.welContainer = $(welContainer);
		htElement.welEditor = _getLabelEditor();
		htElement.welContainer.append(htElement.welEditor);

        // 세부 항목의 엘리먼트 레퍼런스 변수 설정
        htElement.welWrap = $("#custom-label");
        htElement.welColors = htElement.welWrap.find("div.colors");
        _makeColorTable(); // 색상표 생성
        
		htElement.waBtnCustomColor = htElement.welWrap.find("button.issueColor");
        htElement.waCustomLabelInput = htElement.welWrap.find("input"); // color, name, category
		
        htElement.welCustomLabelColor = htElement.welWrap.find("input[name=labelColor]"); // $('#custom-label-color'); 
		htElement.welCustomLabelName =  htElement.welWrap.find("input[name=labelName]");  // $('#custom-label-name');
        htElement.welCustomLabelCategory = htElement.welWrap.find("input[name=labelCategory]"); // $('#custom-label-category');
        htElement.welCustomLabelCategory.typeahead();

        htElement.welBtnCustomLabelSubmit  = htElement.welWrap.find("button.labelSubmit"); //$('#custom-label-submit');
	}
	
	/**
	 * 이벤트 초기화
	 * attach events
	 */
	function _attachEvent(){
		htElement.waBtnCustomColor.click(_onClickBtnCustomColor);
		htElement.welBtnCustomLabelSubmit.click(_onClickBtnSubmitCustom);
		
		htElement.waCustomLabelInput.keypress(_onKeypressInputCustom);
		htElement.waCustomLabelInput.keyup(_onKeyupInputCustom);
		htElement.welCustomLabelColor.keyup(_onKeyupInputColorCustom);		
	}
	
	/**
	 * Get label Editor
	 * 새 라벨 편집기 영역 엘리먼트를 생성해서 반환하는 함수
	 * @return {Wrapped Element} 
	 */
	function _getLabelEditor(){
		// label editor HTML
		var welEditor = $.tmpl(htVar.sTplEditor, {
			"labelAdd"		: Messages("label.add"),
			"labelNew"		: Messages("label.new"),
			"labelName"		: Messages("label.name"),
			"labelCategory"	: Messages('label.category'),
			"labelCustomColor": Messages("label.customColor")
		});
		
		return welEditor;
	}

    function _makeColorTable(){
        var aColorBtns = [];
        htVar.aColors.forEach(function(sColor){
            aColorBtns.push($.tmpl(htVar.sTplBtnColor, {"color": sColor}));
        });
        htElement.welColors.prepend(aColorBtns);
        aColorBtns = null;
    }
    
	/**
	 * 새 라벨 추가 버튼 클릭시 이벤트 핸들러
	 */
	function _onClickBtnSubmitCustom(){
		_addCustomLabel();
	}
	
	/**
	 * 입력폼에서 keypress 이벤트 발생시 이벤트 핸들러
	 * @param {Wrapped Event} weEvt
	 */
	function _onKeypressInputCustom(weEvt){
		return !(weEvt.keyCode === 13);
	}
	
	/**
	 * 입력폼에서 keyup 이벤트 발생시 이벤트 핸들러
	 * @param {Wrapped Event} weEvt
	 */
	function _onKeyupInputCustom(weEvt){
		if(weEvt.keyCode === 13){
			_addCustomLabel();
		}
	}
	
	/**
	 * 새 라벨 추가
     * add custom label
	 */
	function _addCustomLabel(){
		var htData = {
			"name"    : htElement.welCustomLabelName.val(),
			"color"   : htElement.welCustomLabelColor.val(),
			"category": htElement.welCustomLabelCategory.val()
		};
		
		// 하나라도 입력안된 것이 있으면 서버 요청 하지 않음
		if(htData.name.length === 0 || htData.color.length === 0 || htData.category.length === 0){
			return false;
		}
		
		// send request
		$hive.sendForm({
			"sURL"     : htVar.sURLPost, 
			"htData"   : htData,
			"htOptForm": {"enctype": "multipart/form-data"},
			"fOnLoad"  : function(oRes){
				// label.id, label.category, label.name, label.color
				if (!(oRes instanceof Object)) {
					var sMessage = Messages("label.error.creationFailed");
					
					if($(".labels .issue-label:contains('" + htData.name + "')").length > 0){
						sMessage = Messages("label.error.duplicated");
					}
					
					$hive.showAlert(sMessage);
					return;
				}
				
				htElement.welCustomLabelCategory.data("typeahead").source.push(oRes.category);
				htVar.fOnCreate(oRes);
			}
		});
	}
	
	/**
	 * 새 라벨 색상 버튼 클릭시 이벤트 핸들러
	 * @param {Event} eEvt
	 */
	function _onClickBtnCustomColor(eEvt){
		var welTarget = $(eEvt.target || eEvt.srcElement || eEvt.originalTarget);

		// Set clicked button active.
		htElement.waBtnCustomColor.removeClass("active");
		welTarget.addClass("active");

		// Get the selected color.
		var sColor = welTarget.css('background-color');

		// Fill the color input area with the hexadecimal value of
		// the selected color.
		htElement.welCustomLabelColor.val(new RGBColor(sColor).toHex());
        htElement.welCustomLabelColor.css("border-color", sColor);
		_updateSelectedColor(sColor);

		// Focus to the category input area.
		htElement.welCustomLabelCategory.focus();		
	}
	
	/**
	 * 새 라벨 색상 코드 입력 <input>이 업데이트 될 때
	 * 이름 입력 <input> 영역 배경색/글씨색 업데이트 하도록
	 */
	function _onKeyupInputColorCustom(){
		var sColor = htElement.welCustomLabelColor.val();
		var oColor = new RGBColor(sColor);
		
		if (oColor.ok) {
			_updateSelectedColor(sColor);
			htElement.welCustomLabelColor.css("border-color", sColor);
		}
		
		oColor = null;
	}
	
	/**
	 * updateSelectedLabel Color
	 * 지정한 색으로 새 라벨 이름 영역의 배경색을 설정하고
	 * 글자색을 배경색에 맞추어 업데이트 해주는 함수 
	 * @param {String} sBgColor 배경색
	 */
	function _updateSelectedColor(sBgColor){
		// Change the name input area's color to the selected color.
		var sFgColor = $hive.getContrastColor(sBgColor);
		htElement.welCustomLabelName.css({
			"color": sFgColor,
			"background-color": sBgColor
		})

		// Change also place holder's
		// TODO: 이 부분도 나중에 정리할 것. #custom-label-name 고정되어 있음
		var aSelectors = ['#custom-label input[name=labelName]:-moz-placeholder', 
		                 '#custom-label input[name=labelName]:-ms-input-placeholder', 
		                 '#custom-label input[name=labelName]::-webkit-input-placeholder'];

		var elStyle = document.styleSheets[0];
		var sStyleColor = 'color:' + sFgColor + ' !important';
		var sStyleOpacity = 'opacity: 0.8';
		
		aSelectors.forEach(function(sSelector){
			try {
				elStyle.addRule(sSelector, sStyleColor);
				elStyle.addRule(sSelector, sStyleOpacity);
			} catch (e){ }		
		});
		
		aSelectors = elStyle = null;
	}
	
	/**
	 * 카테고리 자동완성(typeahead) 소스에서 지정한 값을 제거함
	 * @param {String} sCategory 제거할 카테고리 이름
	 */
	function _removeCategoryTypeahead(sCategory){
		var aSource = htElement.welCustomLabelCategory.typeahead().data('typeahead').source;
		aSource.pop(aSource.indexOf(sCategory));
	}

    /**
     * 카테고리 자동완성(typeahead) 소스에서 지정한 값을 추가함
     * @param {String} sCategory 추가할 카테고리 이름
     */
    function _addCategoryTypeahead(sCategory) {
        var aSource = htElement.welCustomLabelCategory.typeahead().data('typeahead').source;
        aSource.push(sCategory);
    }

	// 인터페이스 반환
	return {
		"appendTo": _init,
		"removeCategory": _removeCategoryTypeahead,
		"addCategory": _addCategoryTypeahead
	}
})();
