/**
 * @(#)yobi.issue.List.js 2013.03.13
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
			_initVar(htOptions || {})
			_initElement(htOptions || {});
			_attachEvent();
			_initPagination();
			_setLabelColor();
		}
		
		/**
		 * initialize variables except element
		 */
		function _initVar(htOptions){
			htVar.nTotalPages = htOptions.nTotalPages || 1;
            htVar.sIssueCheckBoxesSelector = htOptions.sIssueCheckBoxesSelector;

            htVar.oTypeahead = new yobi.ui.Typeahead("input[name=authorLoginId]", {
				"sActionURL": "/users"
			}) || "";
            
            htVar.oState     = new yobi.ui.Dropdown({"elContainer": htOptions.welState});
            htVar.oAssignee  = new yobi.ui.Dropdown({"elContainer": htOptions.welAssignee});
            htVar.oMilestone = new yobi.ui.Dropdown({"elContainer": htOptions.welMilestone});
            htVar.oAttachingLabel = new yobi.ui.Dropdown({"elContainer": htOptions.welAttachingLabel});
            htVar.oDetachingLabel = new yobi.ui.Dropdown({"elContainer": htOptions.welDetachingLabel});            
		}
		
		/**
		 * initialize element
		 */
		function _initElement(htOptions){
			htElement.welContainer  = $(".inner");
			htElement.welBtnAdvance = $(".btn-advanced");		
			htElement.welPagination = $(htOptions.elPagination || "#pagination");

			htElement.waLabels    = $("a.issue-label[data-color]"); // 목록 > 라벨

            htElement.welMassUpdateForm = htOptions.welMassUpdateForm;
            htElement.welMassUpdateButtons = htOptions.welMassUpdateButtons;
            htElement.welDeleteButton = htOptions.welDeleteButton;
            htElement.waCheckboxes = $(htVar.sIssueCheckBoxesSelector);

      htElement.weAllCheckbox = $('#check-all');      
		}
		
		/**
		 * attach event handlers
		 */
		function _attachEvent(){
			htElement.welBtnAdvance.click(_onClickBtnAdvance);
            htElement.welDeleteButton.click(_onClickBtnDelete);

			// massUpdate dropdowns 
            htVar.oState.onChange(_onChangeUpdateField);
            htVar.oMilestone.onChange(_onChangeUpdateField);
            htVar.oAssignee.onChange(_onChangeUpdateField);
            htVar.oAttachingLabel.onChange(_onChangeUpdateField);
            htVar.oDetachingLabel.onChange(_onChangeUpdateField);

            // massUpdate checkboxes
            htElement.waCheckboxes.change(_onCheckIssue);            
            yobi.ShortcutKey.attach("CTRL+A", function(htInfo){
                htInfo.weEvt.preventDefault();
                $(htElement.weAllCheckbox).trigger('click');
                return false; 
            });
            
            if($(htVar.sIssueCheckBoxesSelector + ':checked').length > 0){
                _onCheckIssue();
            }

            $(htElement.weAllCheckbox).on('click' , function() {
            	var checkedStatus = this.checked;
            	$(htVar.sIssueCheckBoxesSelector).prop('checked', checkedStatus);
        			_onCheckIssue();
            });
		}

        /**
         * Add a hidden input element into the given form.
         */
        function _addFormField(welForm, sName, sValue) {
            $('<input>').attr({
                'type': 'hidden',
                'name': sName,
                'value': sValue
            }).appendTo(welForm);
        }

        /**
         * When check an issue, enable Mass Update dropdowns if only one or
         * more issues are checked, otherwise disable them.
         */
        function _onCheckIssue() {
            htElement.welMassUpdateButtons.attr('disabled', true);
            $(htVar.sIssueCheckBoxesSelector + ':checked').each(function() {
                htElement.welMassUpdateButtons.removeAttr('disabled');
            });
        }

        /**
         * When change the value of any field in the Mass Update form, submit
         * the form and request to update issues.
         */
        function _onChangeUpdateField() {
            var nCnt = 0;
            var welForm = htElement.welMassUpdateForm;
            var fAddCheckedIssueId = function() {
                _addFormField(
                    welForm,
                    'issues[' + (nCnt++) + '].id',
                    $(this).data('issue-id')
                );
            }

            $(htVar.sIssueCheckBoxesSelector + ':checked')
                .each(fAddCheckedIssueId);

            welForm.submit();
        }

		/**
		 * When click the delete button in the Mass Update form
		 */
		function _onClickBtnDelete(){
            _addFormField(htElement.welMassUpdateForm, 'delete', 'true');
            _onChangeUpdateField();
		}

		/**
		 * 상세검색 영역 토글
		 */
		function _onClickBtnAdvance(){
			htElement.welContainer.toggleClass("advanced");
	   	}

		/**
		 * update Pagination
		 * @requires yobi.Pagination
		 */
		function _initPagination(){
		    yobi.Pagination.update(htElement.welPagination, htVar.nTotalPages);
		}
		
		/**
		 * update Label color
		 */
		function _setLabelColor(){
			var welLabel, sColor;
			
			htElement.waLabels.each(function(){
				welLabel = $(this);
				sColor = welLabel.data("color");
				welLabel.css("background-color", sColor);
				welLabel.css("color", $yobi.getContrastColor(sColor));
		    });
			
			welLabel = sColor = null;
		}

		_init(htOptions);
	};
	
})("yobi.issue.List");
